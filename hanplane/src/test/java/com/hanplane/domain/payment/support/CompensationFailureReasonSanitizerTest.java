package com.hanplane.domain.payment.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CompensationFailureReasonSanitizerTest {

    @Test
    void keepsSafeFailureReason() {
        assertThat(CompensationFailureReasonSanitizer.sanitize("PG_TIMEOUT")).isEqualTo("PG_TIMEOUT");
    }

    @Test
    void timeoutMessageBecomesPgTimeout() {
        assertThat(CompensationFailureReasonSanitizer.sanitize("RuntimeException: request timed out with token=secret"))
                .isEqualTo("PG_TIMEOUT");
    }

    @Test
    void connectionMessageBecomesPgConnectionFailed() {
        assertThat(CompensationFailureReasonSanitizer.sanitize("ConnectException: connection refused"))
                .isEqualTo("PG_CONNECTION_FAILED");
    }

    @Test
    void cancelMessageBecomesPgCancelFailed() {
        assertThat(CompensationFailureReasonSanitizer.sanitize("RuntimeException: cancel failed cardNo=1111"))
                .isEqualTo("PG_CANCEL_FAILED");
    }

    @Test
    void unknownRawMessageBecomesCompensationFailed() {
        assertThat(CompensationFailureReasonSanitizer.sanitize("RuntimeException: pg response body includes private data"))
                .isEqualTo("COMPENSATION_FAILED");
    }

    @Test
    void blankMessageBecomesCompensationFailed() {
        assertThat(CompensationFailureReasonSanitizer.sanitize(" "))
                .isEqualTo("COMPENSATION_FAILED");
    }

    @Test
    void longSafeFailureReasonIsTrimmed() {
        String longSafeReason = "A".repeat(101);

        assertThat(CompensationFailureReasonSanitizer.sanitize(longSafeReason))
                .hasSize(100)
                .isEqualTo("A".repeat(100));
    }
}
