package com.hanplane.domain.payment.entity;

import com.hanplane.global.logging.TraceIdFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void updateStatusStoresCurrentTraceId() {
        // given
        MDC.put(TraceIdFilter.TRACE_ID, "trace-current");
        Payment payment = Payment.builder()
                .idempotencyKey("test-key")
                .amount(10000)
                .payStatus(PayStatus.PROCESSING)
                .build();

        // when
        payment.updatePayStatus(PayStatus.FAIL);

        // then
        assertThat(payment.getLastTraceId()).isEqualTo("trace-current");
    }

    @Test
    void updateStatusKeepsLastTraceIdWhenMdcIsEmpty() {
        // given
        Payment payment = Payment.builder()
                .idempotencyKey("test-key")
                .amount(10000)
                .payStatus(PayStatus.PROCESSING)
                .build();
        ReflectionTestUtils.setField(payment, "lastTraceId", "trace-old");

        // when
        payment.updatePayStatus(PayStatus.FAIL);

        // then
        assertThat(payment.getLastTraceId()).isEqualTo("trace-old");
    }

    @Test
    void recordCompensationFailureStoresSanitizedReasonOnly() {
        // given
        Payment payment = Payment.builder()
                .idempotencyKey("test-key")
                .amount(10000)
                .payStatus(PayStatus.CANCEL_REQUIRED)
                .build();

        // when
        payment.recordCompensationFailure("RuntimeException: token=secret-123 cardNo=1111 cancel failed", 3);

        // then
        assertThat(payment.getLastCompensationFailureReason()).isEqualTo("PG_CANCEL_FAILED");
    }

    @Test
    void recordCompensationFailureUsesDefaultReasonWhenBlank() {
        // given
        Payment payment = Payment.builder()
                .idempotencyKey("test-key")
                .amount(10000)
                .payStatus(PayStatus.CANCEL_REQUIRED)
                .build();

        // when
        payment.recordCompensationFailure(" ", 3);

        // then
        assertThat(payment.getLastCompensationFailureReason()).isEqualTo("COMPENSATION_FAILED");
    }
}
