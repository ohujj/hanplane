package com.hanplane.domain.payment.service;

import com.hanplane.domain.order.entity.Order;
import com.hanplane.domain.order.entity.OrderStatus;
import com.hanplane.domain.order.repository.OrderRepository;
import com.hanplane.domain.payment.dto.PaymentConfirmRequest;
import com.hanplane.domain.payment.entity.PayStatus;
import com.hanplane.domain.payment.entity.Payment;
import com.hanplane.domain.payment.repository.PaymentRepository;
import com.hanplane.global.logging.TraceIdFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
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

    @BeforeEach
    void setUp() {
        MDC.put(TraceIdFilter.TRACE_ID, "trace-payment-after-test");
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

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

    @Test
    void PG_조회_결과_불확실_시_Payment는_VERIFY_REQUIRED_Order는_PROCESSING_유지() {
        // given
        Long orderId = 1L;
        String pgPaymentId = "portone-payment-id-001";

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
                .paymentId(pgPaymentId)
                .build();

        // when
        paymentAfterService.verifyRequiredProcess(request);

        // then
        assertThat(payment.getPayStatus()).isEqualTo(PayStatus.VERIFY_REQUIRED);
        assertThat(payment.getPgPaymentId()).isEqualTo(pgPaymentId);
        assertThat(payment.getLastTraceId()).isEqualTo("trace-payment-after-test");
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PROCESSING);
    }

    @Test
    void 금액_불일치_취소_실패_시_Payment는_CANCEL_REQUIRED_Order는_ILLEGAL() {
        // given
        Long orderId = 1L;
        String pgPaymentId = "portone-payment-id-001";

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
                .paymentId(pgPaymentId)
                .build();

        // when
        paymentAfterService.cancelRequiredProcess(request);

        // then
        assertThat(payment.getPayStatus()).isEqualTo(PayStatus.CANCEL_REQUIRED);
        assertThat(payment.getPgPaymentId()).isEqualTo(pgPaymentId);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.ILLEGAL);
    }
}
