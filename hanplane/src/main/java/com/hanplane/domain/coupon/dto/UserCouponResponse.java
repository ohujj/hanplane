package com.hanplane.domain.coupon.dto;

import com.hanplane.domain.coupon.entity.CouponStatus;
import com.hanplane.domain.coupon.entity.UserCoupon;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;

@Getter
@Builder
@Jacksonized
public class UserCouponResponse {
    private final String email;
    private final String userName;
    private final String couponName;
    private final LocalDateTime issuedAt;
    private final LocalDateTime usedAt;
    private final CouponStatus status;

    public static UserCouponResponse from(UserCoupon userCoupon) {
        return UserCouponResponse.builder()
                .couponName(userCoupon.getCoupon().getName())
                .userName(userCoupon.getUser().getName())
                .email(userCoupon.getUser().getEmail())
                .issuedAt(userCoupon.getIssuedAt())
                .status(userCoupon.getStatus())
                .usedAt(userCoupon.getUsedAt())
                .build();
    }

}
