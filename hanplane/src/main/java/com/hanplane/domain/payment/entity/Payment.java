package com.hanplane.domain.payment.entity;

import com.hanplane.domain.order.entity.Order;
import com.hanplane.domain.payment.support.CompensationFailureReasonSanitizer;
import com.hanplane.global.entity.BaseEntity;
import com.hanplane.global.logging.TraceIdHolder;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "payment",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_payment_order_id_idempotency_key",
                        columnNames = {"order_id", "idempotency_key"}
                )
        }
)
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String idempotencyKey;

    @Column
    private String pgPaymentId;

    @Column
    private String transactionId;

    @Column(length = 500)
    private String payMethod;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PayStatus payStatus;

    @Column
    private LocalDateTime paidAt;

    @Column(nullable = false)
    private int amount;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int compensationRetryCount;

    @Column(length = 500)
    private String lastCompensationFailureReason;

    @Column
    private LocalDateTime lastCompensationTriedAt;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean manualReviewRequired;

    @Column(length = 100)
    private String lastTraceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Builder
    public Payment(String idempotencyKey, String pgPaymentId, String transactionId,
                   String payMethod, PayStatus payStatus, LocalDateTime paidAt,
                   int amount, Order order) {
        this.idempotencyKey = idempotencyKey;
        this.pgPaymentId = pgPaymentId;
        this.transactionId = transactionId;
        this.payMethod = payMethod;
        this.payStatus = payStatus;
        this.paidAt = paidAt;
        this.amount = amount;
        this.order = order;
        updateLastTraceId();
    }

    public void updateAfterPay(String pgPaymentId, String transactionId,
                               String payMethod, LocalDateTime paidAt) {
        this.pgPaymentId = pgPaymentId;
        this.transactionId = transactionId;
        this.payMethod = payMethod;
        this.paidAt = paidAt;
        this.payStatus = PayStatus.SUCCESS;
        updateLastTraceId();
    }

    public void updateAfterPay(String transactionId, String payMethod, LocalDateTime paidAt) {
        this.transactionId = transactionId;
        this.payMethod = payMethod;
        this.paidAt = paidAt;
        this.payStatus = PayStatus.SUCCESS;
        updateLastTraceId();
        update();
    }

    public void updatePayStatus(PayStatus payStatus) {
        this.payStatus = payStatus;
        updateLastTraceId();
        update();
    }

    public void updateVerifyRequired(String pgPaymentId) {
        this.pgPaymentId = pgPaymentId;
        this.payStatus = PayStatus.VERIFY_REQUIRED;
        updateLastTraceId();
        update();
    }

    public void updateCancelRequired(String pgPaymentId) {
        this.pgPaymentId = pgPaymentId;
        this.payStatus = PayStatus.CANCEL_REQUIRED;
        updateLastTraceId();
        update();
    }

    public void recordCompensationSuccess() {
        this.lastCompensationTriedAt = LocalDateTime.now();
        updateLastTraceId();
        update();
    }

    public void recordCompensationFailure(String failureReason, int maxRetryCount) {
        this.compensationRetryCount++;
        this.lastCompensationFailureReason = sanitizeFailureReason(failureReason);
        this.lastCompensationTriedAt = LocalDateTime.now();
        if (this.compensationRetryCount >= maxRetryCount) {
            this.manualReviewRequired = true;
        }
        updateLastTraceId();
        update();
    }

    private String sanitizeFailureReason(String failureReason) {
        return CompensationFailureReasonSanitizer.sanitize(failureReason);
    }

    private void updateLastTraceId() {
        String traceId = TraceIdHolder.getTraceId();
        if (traceId != null && !traceId.isBlank()) {
            this.lastTraceId = traceId.length() > 100 ? traceId.substring(0, 100) : traceId;
        }
    }

}
