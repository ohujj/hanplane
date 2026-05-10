package com.hanplane.domain.coupon.entity;

import com.hanplane.domain.user.entity.User;
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
@Table(name = "user_coupon")
public class UserCoupon extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CouponStatus couponStatus;

    @Column()
    private LocalDateTime usedAt;

    @Builder
    public UserCoupon(User user, Coupon coupon, CouponStatus couponStatus) {
        this.user = user;
        this.coupon = coupon;
        this.couponStatus = couponStatus;
    }

    public void updateCouponStatus(CouponStatus couponStatus) {
        this.couponStatus = couponStatus;
        update();
    }
}
