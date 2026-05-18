package com.hanplane.domain.payment.service;

import com.hanplane.domain.order.entity.*;
import com.hanplane.domain.payment.dto.RefundRequest;
import com.hanplane.domain.payment.entity.*;
import com.hanplane.domain.payment.repository.PaymentRepository;
import com.hanplane.domain.payment.repository.RefundRepository;
import com.hanplane.domain.product.entity.Product;
import com.hanplane.domain.user.entity.Role;
import com.hanplane.domain.user.entity.User;
import com.hanplane.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RefundSaveServiceTest {

    @Mock
    private RefundRepository refundRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @InjectMocks
    private RefundSaveService refundSaveService;

    private User user;
    private Order order;
    private OrderItem orderItem;
    private Payment payment;

    @BeforeEach
    void setUp() {
        user = User.builder().email("test@test.com").password("1234").role(Role.USER).name("테스트").build();
        ReflectionTestUtils.setField(user, "id", 1L);

        Product product = Product.builder()
                .name("한정판").price(10000).totalQuantity(10).availQuantity(7)
                .expiredAt(LocalDateTime.now().plusMonths(6)).build();
        ReflectionTestUtils.setField(product, "id", 1L);

        order = Order.builder()
                .user(user).coupon(null).totalPrice(30000).orderStatus(OrderStatus.PAID).build();
        ReflectionTestUtils.setField(order, "id", 1L);

        // price = 단가(10,000), quantity = 3
        orderItem = OrderItem.builder()
                .product(product).order(order).price(10000).quantity(3).build();
        ReflectionTestUtils.setField(orderItem, "id", 1L);

        List<OrderItem> orderItems = new ArrayList<>();
        orderItems.add(orderItem);
        ReflectionTestUtils.setField(order, "orderItems", orderItems);

        payment = Payment.builder()
                .order(order).idempotencyKey("test-key").amount(30000)
                .payStatus(PayStatus.SUCCESS).build();
        ReflectionTestUtils.setField(payment, "id", 1L);
        ReflectionTestUtils.setField(payment, "pgPaymentId", "pg-test-id");
    }

    @Test
    void 수량_3개_환불_금액은_30000() {
        given(paymentRepository.findByIdWithOrderAndItems(1L)).willReturn(Optional.of(payment));
        given(refundRepository.save(any(Refund.class))).willAnswer(inv -> inv.getArgument(0));

        RefundRequest request = RefundRequest.builder()
                .paymentId(1L)
                .orderItemIds(List.of(1L))
                .build();

        Refund refund = refundSaveService.refundSaveProcess(1L, request);

        // 단가(10,000) * 수량(3) = 30,000
        assertThat(refund.getAmount()).isEqualTo(30000);
        assertThat(refund.getStatus()).isEqualTo(RefundStatus.PENDING);
    }

    @Test
    void 이미_REFUNDED_상태인_OrderItem은_다시_환불_불가() {
        orderItem.updateStatus(OrderItemStatus.REFUNDED);
        given(paymentRepository.findByIdWithOrderAndItems(1L)).willReturn(Optional.of(payment));

        RefundRequest request = RefundRequest.builder()
                .paymentId(1L)
                .orderItemIds(List.of(1L))
                .build();

        assertThrows(BusinessException.class, () ->
                refundSaveService.refundSaveProcess(1L, request));
    }

    @Test
    void 결제상태가_SUCCESS가_아니면_환불_불가() {
        ReflectionTestUtils.setField(payment, "payStatus", PayStatus.PENDING);
        given(paymentRepository.findByIdWithOrderAndItems(1L)).willReturn(Optional.of(payment));

        RefundRequest request = RefundRequest.builder()
                .paymentId(1L)
                .orderItemIds(List.of(1L))
                .build();

        assertThrows(BusinessException.class, () ->
                refundSaveService.refundSaveProcess(1L, request));
    }

    @Test
    void 다른_유저의_주문은_환불_불가() {
        given(paymentRepository.findByIdWithOrderAndItems(1L)).willReturn(Optional.of(payment));

        RefundRequest request = RefundRequest.builder()
                .paymentId(1L)
                .orderItemIds(List.of(1L))
                .build();

        // userId 99L ≠ 주문 소유자 1L
        assertThrows(BusinessException.class, () ->
                refundSaveService.refundSaveProcess(99L, request));
    }
}