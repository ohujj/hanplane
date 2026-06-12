package com.hanplane.domain.payment.service;

import com.hanplane.domain.order.repository.OrderRepository;
import com.hanplane.domain.payment.dto.PaymentConfirmRequest;
import com.hanplane.domain.payment.entity.Payment;
import com.hanplane.global.exception.BusinessException;
import com.hanplane.global.exception.ErrorCode;
import io.portone.sdk.server.PortOneClient;
import io.portone.sdk.server.payment.CancelPaymentResponse;
import io.portone.sdk.server.payment.PaidPayment;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceConfirmTest {

    @InjectMocks
    private PaymentService paymentService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentConfirmService paymentConfirmService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PortOneClient portOneClient;

    @Mock
    private PaymentAfterService paymentAfterService;

    private final Long userId = 1L;
    private final String idempotencyKey = "existing-idem-key";

    private PaymentConfirmRequest request;

    @BeforeEach
    void setUp() {
        request = PaymentConfirmRequest.builder()
                .orderId(10L)
                .paymentId("portone-payment-id-001")
                .build();
    }

    @Test
    @DisplayName("기존 Payment가 있는 경우(newlyCreated=false) PG 호출 없이 바로 종료된다")
    void 기존_Payment_있으면_PG_호출_안함() {
        // given
        Payment existingPayment = mock(Payment.class);
        PaymentConfirmResult existingResult = new PaymentConfirmResult(existingPayment, false);

        when(paymentConfirmService.confirmOrder(userId, request, idempotencyKey))
                .thenReturn(existingResult);

        // when
        paymentService.confirm(userId, request, idempotencyKey);

        // then
        // portOneClient가 전혀 호출되지 않으면 payProcess()가 스킵된 것
        verifyNoInteractions(portOneClient);
        verifyNoInteractions(paymentAfterService);
    }

    @Test
    @DisplayName("PG lookup failure marks Payment as VERIFY_REQUIRED")
    void pgLookupFailureMarksPaymentAsVerifyRequired() throws Exception {
        // given
        Payment newPayment = mock(Payment.class);
        PaymentConfirmResult newResult = new PaymentConfirmResult(newPayment, true);

        when(paymentConfirmService.confirmOrder(userId, request, idempotencyKey))
                .thenReturn(newResult);
        when(portOneClient.getPayment().getPayment(request.getPaymentId()).get())
                .thenThrow(new RuntimeException("pg timeout"));

        // when
        BusinessException exception = Assertions.assertThrows(
                BusinessException.class,
                () -> paymentService.confirm(userId, request, idempotencyKey)
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PG_CALL_FAILED);
        verify(paymentAfterService).verifyRequiredProcess(request);
        verify(paymentAfterService, never()).payExceptionProcess(request);
        verify(paymentAfterService, never()).payAfterProcess(any(), any());
    }

    @Test
    @DisplayName("amount mismatch cancels PG payment and marks request as illegal when cancel succeeds")
    void amountMismatchCancelSuccessMarksIllegal() throws Exception {
        // given
        Payment newPayment = mock(Payment.class);
        PaidPayment paidPayment = mock(PaidPayment.class, Answers.RETURNS_DEEP_STUBS);
        CancelPaymentResponse cancelResponse = mock(CancelPaymentResponse.class);

        when(newPayment.getAmount()).thenReturn(10000);
        when(paidPayment.getAmount().getTotal()).thenReturn(20000L);
        when(paymentConfirmService.confirmOrder(userId, request, idempotencyKey))
                .thenReturn(new PaymentConfirmResult(newPayment, true));
        when(portOneClient.getPayment().getPayment(request.getPaymentId()).get())
                .thenReturn(paidPayment);
        when(portOneClient.getPayment().cancelPayment(
                eq(request.getPaymentId()),
                eq(20000L),
                isNull(),
                isNull(),
                eq("Amount mismatch auto cancel"),
                isNull(),
                isNull(),
                isNull(),
                isNull()
        )).thenReturn(CompletableFuture.completedFuture(cancelResponse));

        // when
        BusinessException exception = Assertions.assertThrows(
                BusinessException.class,
                () -> paymentService.confirm(userId, request, idempotencyKey)
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        verify(paymentAfterService).illegalRequestProcess(request);
        verify(paymentAfterService, never()).cancelRequiredProcess(request);
        verify(paymentAfterService, never()).payAfterProcess(any(), any());
    }

    @Test
    @DisplayName("amount mismatch marks Payment as CANCEL_REQUIRED when PG cancel fails")
    void amountMismatchCancelFailureMarksCancelRequired() throws Exception {
        // given
        Payment newPayment = mock(Payment.class);
        PaidPayment paidPayment = mock(PaidPayment.class, Answers.RETURNS_DEEP_STUBS);

        when(newPayment.getAmount()).thenReturn(10000);
        when(paidPayment.getAmount().getTotal()).thenReturn(20000L);
        when(paymentConfirmService.confirmOrder(userId, request, idempotencyKey))
                .thenReturn(new PaymentConfirmResult(newPayment, true));
        when(portOneClient.getPayment().getPayment(request.getPaymentId()).get())
                .thenReturn(paidPayment);
        when(portOneClient.getPayment().cancelPayment(
                eq(request.getPaymentId()),
                eq(20000L),
                isNull(),
                isNull(),
                eq("Amount mismatch auto cancel"),
                isNull(),
                isNull(),
                isNull(),
                isNull()
        )).thenReturn(CompletableFuture.failedFuture(new RuntimeException("cancel failed")));

        // when
        BusinessException exception = Assertions.assertThrows(
                BusinessException.class,
                () -> paymentService.confirm(userId, request, idempotencyKey)
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        verify(paymentAfterService).cancelRequiredProcess(request);
        verify(paymentAfterService, never()).illegalRequestProcess(request);
        verify(paymentAfterService, never()).payAfterProcess(any(), any());
    }
}
