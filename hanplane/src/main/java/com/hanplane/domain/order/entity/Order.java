package com.hanplane.domain.order.entity;

import com.hanplane.domain.coupon.entity.Coupon;
import com.hanplane.domain.user.entity.User;
import com.hanplane.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "orders")
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = true)
    private Coupon coupon;

    @Column(nullable = false)
    private int totalPrice;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY) // 1 대 다 관계, 매핑, lazy로 해서 n+1 문제 제어
    private List<OrderItem> orderItems = new ArrayList<>();

    @Builder
    private Order(User user, Coupon coupon, int totalPrice, OrderStatus orderStatus) {
        this.user = user;
        this.coupon = coupon;
        this.totalPrice = totalPrice;
        this.orderStatus = orderStatus;
    }

    public void updateOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
        update();
    }
}
