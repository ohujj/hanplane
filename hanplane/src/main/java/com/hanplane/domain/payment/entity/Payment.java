package com.hanplane.domain.payment.entity;

import com.hanplane.domain.order.entity.Order;
import com.hanplane.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "payment")
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
    }

    public void updateAfterPay(String pgPaymentId, String transactionId,
                               String payMethod, LocalDateTime paidAt) {
        this.pgPaymentId = pgPaymentId;
        this.transactionId = transactionId;
        this.payMethod = payMethod;
        this.paidAt = paidAt;
        this.payStatus = PayStatus.SUCCESS;
    }

    public void updatePayStatus(PayStatus payStatus) {
        this.payStatus = payStatus;
        update();
    }

}