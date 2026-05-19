package com.hanplane.domain.payment.service;

import com.hanplane.domain.order.entity.Order;
import com.hanplane.domain.order.entity.OrderStatus;
import com.hanplane.domain.order.repository.OrderRepository;
import com.hanplane.domain.payment.dto.PaymentConfirmRequest;
import com.hanplane.domain.payment.entity.PayStatus;
import com.hanplane.domain.payment.entity.Payment;
import com.hanplane.domain.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PaymentAfterServiceTest {

    @InjectMocks
    private PaymentAfterService paymentAfterService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Test
    void PG_호출_실패_시_Payment는_FAIL_Order는_PENDING으로_복구() {
        // given
        Long orderId = 1L;

        Payment payment = Payment.builder()
                .idempotencyKey("test-key")
                .amount(30000)
                .payStatus(PayStatus.PROCESSING)
                .build();
        ReflectionTestUtils.setField(payment, "id", 1L);

        Order order = Order.builder()
                .totalPrice(30000)
                .orderStatus(OrderStatus.PROCESSING)
                .build();
        ReflectionTestUtils.setField(order, "id", orderId);

        given(paymentRepository.findByOrderIdAndPayStatus(orderId, PayStatus.PROCESSING))
                .willReturn(Optional.of(payment));
        given(orderRepository.findById(orderId))
                .willReturn(Optional.of(order));

        PaymentConfirmRequest request = PaymentConfirmRequest.builder()
                .orderId(orderId)
                .build();

        // when
        paymentAfterService.payExceptionProcess(request);

        // then
        // PG 실패 → Payment FAIL, Order PENDING (재결제 가능 상태)
        assertThat(payment.getPayStatus()).isEqualTo(PayStatus.FAIL);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
    }
}