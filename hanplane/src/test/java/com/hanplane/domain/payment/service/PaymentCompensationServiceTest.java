package com.hanplane.domain.payment.service;

import com.hanplane.domain.order.entity.Order;
import com.hanplane.domain.order.entity.OrderStatus;
import com.hanplane.domain.payment.entity.PayStatus;
import com.hanplane.domain.payment.entity.Payment;
import com.hanplane.domain.payment.repository.PaymentRepository;
import com.hanplane.global.logging.TraceIdFilter;
import io.portone.sdk.server.PortOneClient;
import io.portone.sdk.server.payment.CancelPaymentResponse;
import io.portone.sdk.server.payment.PaidPayment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.Pageable;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentCompensationServiceTest {

    @InjectMocks
    private PaymentCompensationService paymentCompensationService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PortOneClient portOneClient;

    @BeforeEach
    void setUp() {
        MDC.put(TraceIdFilter.TRACE_ID, "trace-compensation-test");
        ReflectionTestUtils.setField(paymentCompensationService, "batchSize", 100);
        ReflectionTestUtils.setField(paymentCompensationService, "maxRetryCount", 3);
        ReflectionTestUtils.setField(paymentCompensationService, "retryIntervalMinutes", 5L);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void cancelRequiredPaymentCancelSuccessMarksIllegal() {
        // given
        Payment payment = payment(1L, "portone-payment-id-001", PayStatus.CANCEL_REQUIRED, 10000, order(OrderStatus.ILLEGAL));
        CancelPaymentResponse response = mock(CancelPaymentResponse.class);

        mockTargets(PayStatus.CANCEL_REQUIRED, payment);
        mockCancelSuccess("portone-payment-id-001", "Amount mismatch cancel retry", response);

        // when
        paymentCompensationService.retryCancelRequiredPayments();

        // then
        assertThat(payment.getPayStatus()).isEqualTo(PayStatus.ILLEGAL);
        assertThat(payment.getLastCompensationTriedAt()).isNotNull();
        assertThat(payment.getLastTraceId()).isEqualTo("trace-compensation-test");
    }

    @Test
    void cancelRequiredPaymentCancelFailureKeepsCancelRequired() {
        // given
        Payment payment = payment(1L, "portone-payment-id-001", PayStatus.CANCEL_REQUIRED, 10000, order(OrderStatus.ILLEGAL));

        mockTargets(PayStatus.CANCEL_REQUIRED, payment);
        mockCancelFailure("portone-payment-id-001", "Amount mismatch cancel retry");

        // when
        paymentCompensationService.retryCancelRequiredPayments();

        // then
        assertThat(payment.getPayStatus()).isEqualTo(PayStatus.CANCEL_REQUIRED);
        assertThat(payment.getCompensationRetryCount()).isEqualTo(1);
        assertThat(payment.getLastCompensationFailureReason()).isEqualTo("PG_CANCEL_FAILED");
        assertThat(payment.getLastCompensationTriedAt()).isNotNull();
        assertThat(payment.isManualReviewRequired()).isFalse();
    }

    @Test
    void verifyRequiredPaidPaymentWithSameAmountMarksSuccessAndPaid() throws Exception {
        // given
        Order order = order(OrderStatus.PROCESSING);
        Payment payment = payment(1L, "portone-payment-id-001", PayStatus.VERIFY_REQUIRED, 10000, order);
        PaidPayment paidPayment = paidPayment(10000L);

        mockTargets(PayStatus.VERIFY_REQUIRED, payment);
        when(portOneClient.getPayment().getPayment("portone-payment-id-001").get())
                .thenReturn(paidPayment);

        // when
        paymentCompensationService.retryVerifyRequiredPayments();

        // then
        assertThat(payment.getPayStatus()).isEqualTo(PayStatus.SUCCESS);
        assertThat(payment.getTransactionId()).isEqualTo("tx-001");
        assertThat(payment.getPayMethod()).isEqualTo("UNKNOWN");
        assertThat(payment.getLastCompensationTriedAt()).isNotNull();
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void verifyRequiredNonPaidPaymentMarksFailAndPending() throws Exception {
        // given
        Order order = order(OrderStatus.PROCESSING);
        Payment payment = payment(1L, "portone-payment-id-001", PayStatus.VERIFY_REQUIRED, 10000, order);
        io.portone.sdk.server.payment.Payment nonPaidPayment = mock(io.portone.sdk.server.payment.Payment.class);

        mockTargets(PayStatus.VERIFY_REQUIRED, payment);
        when(portOneClient.getPayment().getPayment("portone-payment-id-001").get())
                .thenReturn(nonPaidPayment);

        // when
        paymentCompensationService.retryVerifyRequiredPayments();

        // then
        assertThat(payment.getPayStatus()).isEqualTo(PayStatus.FAIL);
        assertThat(payment.getLastCompensationTriedAt()).isNotNull();
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void verifyRequiredLookupFailureKeepsVerifyRequired() throws Exception {
        // given
        Order order = order(OrderStatus.PROCESSING);
        Payment payment = payment(1L, "portone-payment-id-001", PayStatus.VERIFY_REQUIRED, 10000, order);

        mockTargets(PayStatus.VERIFY_REQUIRED, payment);
        when(portOneClient.getPayment().getPayment("portone-payment-id-001").get())
                .thenThrow(new RuntimeException("pg timeout"));

        // when
        paymentCompensationService.retryVerifyRequiredPayments();

        // then
        assertThat(payment.getPayStatus()).isEqualTo(PayStatus.VERIFY_REQUIRED);
        assertThat(payment.getCompensationRetryCount()).isEqualTo(1);
        assertThat(payment.getLastCompensationFailureReason()).isEqualTo("PG_TIMEOUT");
        assertThat(payment.getLastCompensationTriedAt()).isNotNull();
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PROCESSING);
    }

    @Test
    void verifyRequiredAmountMismatchCancelSuccessMarksIllegal() throws Exception {
        // given
        Order order = order(OrderStatus.PROCESSING);
        Payment payment = payment(1L, "portone-payment-id-001", PayStatus.VERIFY_REQUIRED, 10000, order);
        PaidPayment paidPayment = paidPaymentAmountOnly(20000L);
        CancelPaymentResponse response = mock(CancelPaymentResponse.class);

        mockTargets(PayStatus.VERIFY_REQUIRED, payment);
        when(portOneClient.getPayment().getPayment("portone-payment-id-001").get())
                .thenReturn(paidPayment);
        mockCancelSuccess("portone-payment-id-001", "Verify required amount mismatch cancel", response);

        // when
        paymentCompensationService.retryVerifyRequiredPayments();

        // then
        assertThat(payment.getPayStatus()).isEqualTo(PayStatus.ILLEGAL);
        assertThat(payment.getLastCompensationTriedAt()).isNotNull();
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.ILLEGAL);
    }

    @Test
    void verifyRequiredAmountMismatchCancelFailureMarksCancelRequired() throws Exception {
        // given
        Order order = order(OrderStatus.PROCESSING);
        Payment payment = payment(1L, "portone-payment-id-001", PayStatus.VERIFY_REQUIRED, 10000, order);
        PaidPayment paidPayment = paidPaymentAmountOnly(20000L);

        mockTargets(PayStatus.VERIFY_REQUIRED, payment);
        when(portOneClient.getPayment().getPayment("portone-payment-id-001").get())
                .thenReturn(paidPayment);
        mockCancelFailure("portone-payment-id-001", "Verify required amount mismatch cancel");

        // when
        paymentCompensationService.retryVerifyRequiredPayments();

        // then
        assertThat(payment.getPayStatus()).isEqualTo(PayStatus.CANCEL_REQUIRED);
        assertThat(payment.getCompensationRetryCount()).isEqualTo(1);
        assertThat(payment.getLastCompensationFailureReason()).isEqualTo("PG_CANCEL_FAILED");
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.ILLEGAL);
    }

    @Test
    void compensationFailureReachingMaxRetryMarksManualReviewRequired() {
        // given
        Payment payment = payment(1L, "portone-payment-id-001", PayStatus.CANCEL_REQUIRED, 10000, order(OrderStatus.ILLEGAL));
        ReflectionTestUtils.setField(payment, "compensationRetryCount", 2);

        mockTargets(PayStatus.CANCEL_REQUIRED, payment);
        mockCancelFailure("portone-payment-id-001", "Amount mismatch cancel retry");

        // when
        paymentCompensationService.retryCancelRequiredPayments();

        // then
        assertThat(payment.getCompensationRetryCount()).isEqualTo(3);
        assertThat(payment.isManualReviewRequired()).isTrue();
    }

    private void mockCancelSuccess(String pgPaymentId, String reason, CancelPaymentResponse response) {
        when(portOneClient.getPayment().cancelPayment(
                eq(pgPaymentId),
                isNull(),
                isNull(),
                isNull(),
                eq(reason),
                isNull(),
                isNull(),
                isNull(),
                isNull()
        )).thenReturn(CompletableFuture.completedFuture(response));
    }

    private void mockCancelFailure(String pgPaymentId, String reason) {
        when(portOneClient.getPayment().cancelPayment(
                eq(pgPaymentId),
                isNull(),
                isNull(),
                isNull(),
                eq(reason),
                isNull(),
                isNull(),
                isNull(),
                isNull()
        )).thenReturn(CompletableFuture.failedFuture(new RuntimeException("cancel failed")));
    }

    private void mockTargets(PayStatus payStatus, Payment payment) {
        when(paymentRepository.findCompensationTargets(
                eq(payStatus),
                eq(3),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(List.of(payment));
    }

    private PaidPayment paidPayment(Long amount) {
        PaidPayment paidPayment = mock(PaidPayment.class, Answers.RETURNS_DEEP_STUBS);
        when(paidPayment.getAmount().getTotal()).thenReturn(amount);
        when(paidPayment.getPaidAt()).thenReturn(Instant.parse("2026-06-10T12:00:00Z"));
        when(paidPayment.getTransactionId()).thenReturn("tx-001");
        when(paidPayment.getMethod()).thenReturn(null);
        return paidPayment;
    }

    private PaidPayment paidPaymentAmountOnly(Long amount) {
        PaidPayment paidPayment = mock(PaidPayment.class, Answers.RETURNS_DEEP_STUBS);
        when(paidPayment.getAmount().getTotal()).thenReturn(amount);
        return paidPayment;
    }

    private Order order(OrderStatus orderStatus) {
        Order order = Order.builder()
                .totalPrice(10000)
                .orderStatus(orderStatus)
                .build();
        ReflectionTestUtils.setField(order, "id", 1L);
        return order;
    }

    private Payment payment(Long id, String pgPaymentId, PayStatus payStatus, int amount, Order order) {
        Payment payment = Payment.builder()
                .idempotencyKey("test-key")
                .pgPaymentId(pgPaymentId)
                .amount(amount)
                .payStatus(payStatus)
                .order(order)
                .build();
        ReflectionTestUtils.setField(payment, "id", id);
        return payment;
    }
}
