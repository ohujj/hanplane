package com.hanplane.domain.coupon.entity;

import com.hanplane.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_coupon")
public class UserCoupon {
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
    private CouponStatus status;

    @Column(nullable = false)
    private LocalDateTime issuedAt ;

    @Column()
    private LocalDateTime usedAt;

    @Builder
    public UserCoupon(User user, Coupon coupon, CouponStatus status, LocalDateTime issuedAt) {
        this.user = user;
        this.coupon = coupon;
        this.status = status;
        this.issuedAt = LocalDateTime.now();
    }
}
