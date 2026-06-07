package com.hanplane.domain.payment.service;

import com.hanplane.domain.order.entity.Order;
import com.hanplane.domain.order.entity.OrderStatus;
import com.hanplane.domain.order.repository.OrderRepository;
import com.hanplane.domain.payment.dto.PaymentConfirmRequest;
import com.hanplane.domain.payment.entity.PayStatus;
import com.hanplane.domain.payment.entity.Payment;
import com.hanplane.domain.payment.repository.PaymentRepository;
import com.hanplane.domain.user.entity.User;
import com.hanplane.global.exception.BusinessException;
import com.hanplane.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentConfirmServiceTest {

    @InjectMocks
    private PaymentConfirmService paymentConfirmService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRepository paymentRepository;

    private final Long userId = 1L;
    private final Long orderId = 10L;
    private final String idempotencyKey = "test-idem-key-uuid";

    private Order order;
    private User user;
    private PaymentConfirmRequest request;

    @BeforeEach
    void setUp() {
        user = mock(User.class);
        when(user.getId()).thenReturn(userId);

        order = mock(Order.class);
        when(order.getId()).thenReturn(orderId);
        when(order.getUser()).thenReturn(user);
        when(order.getTotalPrice()).thenReturn(10000);

        request = PaymentConfirmRequest.builder()
                .orderId(orderId)
                .paymentId("portone-payment-id-001")
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
    }

    @Test
    @DisplayName("같은 orderId + idempotencyKey로 호출 시 기존 Payment를 반환하고 새로 저장하지 않는다")
    void 중복_요청_시_기존_Payment_반환_저장_안함() {
        // given
        Payment existingPayment = Payment.builder()
                .idempotencyKey(idempotencyKey)
                .payStatus(PayStatus.PROCESSING)
                .amount(10000)
                .order(order)
                .build();

        when(paymentRepository.findByOrder_IdAndIdempotencyKey(orderId, idempotencyKey))
                .thenReturn(Optional.of(existingPayment));

        // when
        PaymentConfirmResult result = paymentConfirmService.confirmOrder(userId, request, idempotencyKey);

        // then
        assertThat(result.newlyCreated()).isFalse();
        assertThat(result.payment()).isSameAs(existingPayment);

        // saveAndFlush는 호출되면 안 됨
        verify(paymentRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("새 요청(기존 Payment 없음, PENDING 주문)이면 idempotencyKey를 헤더값으로 저장한다")
    void 새_Payment_idempotencyKey_헤더값으로_저장() {
        // given
        when(order.getOrderStatus()).thenReturn(OrderStatus.PENDING);
        when(paymentRepository.findByOrder_IdAndIdempotencyKey(orderId, idempotencyKey))
                .thenReturn(Optional.empty());

        // saveAndFlush가 받은 Payment 인스턴스를 그대로 반환
        when(paymentRepository.saveAndFlush(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        PaymentConfirmResult result = paymentConfirmService.confirmOrder(userId, request, idempotencyKey);

        // then
        assertThat(result.newlyCreated()).isTrue();
        assertThat(result.payment().getIdempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(result.payment().getPayStatus()).isEqualTo(PayStatus.PROCESSING);

        verify(paymentRepository, times(1)).saveAndFlush(any(Payment.class));
    }

    @Test
    @DisplayName("기존 Payment 없는 상태에서 주문이 PROCESSING이면 ORDER_STATUS_IS_NOT_PENDING 예외를 던진다")
    void 기존Payment없음_PROCESSING주문_예외발생() {
        // given
        when(order.getOrderStatus()).thenReturn(OrderStatus.PROCESSING);
        when(paymentRepository.findByOrder_IdAndIdempotencyKey(orderId, idempotencyKey))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> paymentConfirmService.confirmOrder(userId, request, idempotencyKey))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_STATUS_IS_NOT_PENDING);

        verify(paymentRepository, never()).saveAndFlush(any());
    }
}