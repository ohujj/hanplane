package com.hanplane.domain.payment.support;

public final class CompensationFailureReasonSanitizer {

    private static final String DEFAULT_FAILURE_REASON = "COMPENSATION_FAILED";
    private static final String PG_CANCEL_FAILED = "PG_CANCEL_FAILED";
    private static final String PG_CONNECTION_FAILED = "PG_CONNECTION_FAILED";
    private static final String PG_TIMEOUT = "PG_TIMEOUT";
    private static final int MAX_FAILURE_REASON_LENGTH = 100;
    private static final String SAFE_REASON_PATTERN = "[A-Z0-9_:-]+";

    private CompensationFailureReasonSanitizer() {
    }

    public static String sanitize(String failureReason) {
        if (failureReason == null || failureReason.isBlank()) {
            return DEFAULT_FAILURE_REASON;
        }

        String trimmed = failureReason.trim();
        if (trimmed.matches(SAFE_REASON_PATTERN)) {
            return trimLength(trimmed);
        }

        String lower = trimmed.toLowerCase();
        if (lower.contains("timeout") || lower.contains("timed out")) {
            return PG_TIMEOUT;
        }
        if (lower.contains("connect") || lower.contains("connection")) {
            return PG_CONNECTION_FAILED;
        }
        if (lower.contains("cancel")) {
            return PG_CANCEL_FAILED;
        }

        return DEFAULT_FAILURE_REASON;
    }

    private static String trimLength(String failureReason) {
        return failureReason.length() > MAX_FAILURE_REASON_LENGTH
                ? failureReason.substring(0, MAX_FAILURE_REASON_LENGTH)
                : failureReason;
    }
}
