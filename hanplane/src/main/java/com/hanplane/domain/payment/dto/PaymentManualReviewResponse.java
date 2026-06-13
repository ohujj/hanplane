package com.hanplane.domain.payment.dto;

import com.hanplane.domain.payment.entity.PayStatus;
import com.hanplane.domain.payment.entity.Payment;
import com.hanplane.domain.payment.support.CompensationFailureReasonSanitizer;

import java.time.LocalDateTime;

public record PaymentManualReviewResponse(
        Long paymentId,
        Long orderId,
        PayStatus payStatus,
        String pgPaymentId,
        int amount,
        int compensationRetryCount,
        String lastCompensationFailureReason,
        LocalDateTime lastCompensationTriedAt,
        String lastTraceId,
        boolean manualReviewRequired,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static PaymentManualReviewResponse from(Payment payment) {
        return new PaymentManualReviewResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getPayStatus(),
                payment.getPgPaymentId(),
                payment.getAmount(),
                payment.getCompensationRetryCount(),
                CompensationFailureReasonSanitizer.sanitize(payment.getLastCompensationFailureReason()),
                payment.getLastCompensationTriedAt(),
                payment.getLastTraceId(),
                payment.isManualReviewRequired(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
