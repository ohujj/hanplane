package com.hanplane.domain.payment.service;

import com.hanplane.domain.order.repository.OrderRepository;
import com.hanplane.domain.payment.dto.PaymentConfirmRequest;
import com.hanplane.domain.payment.entity.Payment;
import io.portone.sdk.server.PortOneClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceConfirmTest {

    @InjectMocks
    private PaymentService paymentService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentConfirmService paymentConfirmService;

    @Mock
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
}