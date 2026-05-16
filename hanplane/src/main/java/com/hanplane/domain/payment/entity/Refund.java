package com.hanplane.domain.payment.entity;

import com.hanplane.domain.order.entity.Order;
import com.hanplane.domain.order.entity.OrderItem;
import com.hanplane.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "refund")
public class Refund extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @ManyToMany
    @JoinTable(
            name = "refund_order_item",
            joinColumns = @JoinColumn(name = "refund_id"),
            inverseJoinColumns = @JoinColumn(name = "orderitem_id")
    )
    private List<OrderItem> orderItems;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RefundStatus status;

    @Column(nullable = false)
    private LocalDateTime refundAt;

    @Column(nullable = false)
    private int amount;

    @Builder
    public Refund(Payment payment, List<OrderItem> orderItems, RefundStatus status, int amount) {
        this.payment = payment;
        this.orderItems = orderItems;
        this.status = status;
        this.refundAt = LocalDateTime.now();
        this.amount = amount;
    }


    public void updateRefundStatus(RefundStatus status) {
        this.status = status;
        update();
    }
}
