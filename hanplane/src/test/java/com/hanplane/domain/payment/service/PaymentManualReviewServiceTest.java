package com.hanplane.domain.payment.service;

import com.hanplane.domain.order.entity.Order;
import com.hanplane.domain.payment.dto.PaymentManualReviewResponse;
import com.hanplane.domain.payment.entity.PayStatus;
import com.hanplane.domain.payment.entity.Payment;
import com.hanplane.domain.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentManualReviewServiceTest {

    @InjectMocks
    private PaymentManualReviewService paymentManualReviewService;

    @Mock
    private PaymentRepository paymentRepository;

    @Test
    void manualReviewResponseContainsOperationalFields() {
        // given
        PageRequest pageable = PageRequest.of(0, 10);
        Order order = Order.builder()
                .totalPrice(10000)
                .build();
        ReflectionTestUtils.setField(order, "id", 10L);

        Payment payment = Payment.builder()
                .idempotencyKey("test-key")
                .pgPaymentId("pg-payment-001")
                .payStatus(PayStatus.CANCEL_REQUIRED)
                .amount(10000)
                .order(order)
                .build();
        LocalDateTime triedAt = LocalDateTime.of(2026, 6, 12, 20, 0);
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 12, 19, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 6, 12, 20, 5);

        ReflectionTestUtils.setField(payment, "id", 1L);
        ReflectionTestUtils.setField(payment, "compensationRetryCount", 5);
        ReflectionTestUtils.setField(payment, "lastCompensationFailureReason", "RuntimeException: cancel failed");
        ReflectionTestUtils.setField(payment, "lastCompensationTriedAt", triedAt);
        ReflectionTestUtils.setField(payment, "lastTraceId", "batch-trace-001");
        ReflectionTestUtils.setField(payment, "manualReviewRequired", true);
        ReflectionTestUtils.setField(payment, "createdAt", createdAt);
        ReflectionTestUtils.setField(payment, "updatedAt", updatedAt);

        when(paymentRepository.findManualReviewPayments(PayStatus.CANCEL_REQUIRED, pageable))
                .thenReturn(new PageImpl<>(List.of(payment), pageable, 1));

        // when
        Page<PaymentManualReviewResponse> result =
                paymentManualReviewService.getManualReviewPayments(PayStatus.CANCEL_REQUIRED, pageable);

        // then
        PaymentManualReviewResponse response = result.getContent().get(0);
        assertThat(response.paymentId()).isEqualTo(1L);
        assertThat(response.orderId()).isEqualTo(10L);
        assertThat(response.payStatus()).isEqualTo(PayStatus.CANCEL_REQUIRED);
        assertThat(response.pgPaymentId()).isEqualTo("pg-payment-001");
        assertThat(response.amount()).isEqualTo(10000);
        assertThat(response.compensationRetryCount()).isEqualTo(5);
        assertThat(response.lastCompensationFailureReason()).isEqualTo("PG_CANCEL_FAILED");
        assertThat(response.lastCompensationTriedAt()).isEqualTo(triedAt);
        assertThat(response.lastTraceId()).isEqualTo("batch-trace-001");
        assertThat(response.manualReviewRequired()).isTrue();
        assertThat(response.createdAt()).isEqualTo(createdAt);
        assertThat(response.updatedAt()).isEqualTo(updatedAt);
    }
}
