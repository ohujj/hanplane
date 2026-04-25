package com.hanplane.domain.coupon.dto;

import com.hanplane.domain.coupon.CouponDocument;
import com.hanplane.domain.coupon.entity.Coupon;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;

@Getter
@Builder
@Jacksonized
public class CouponListResponse {

    private final Long couponId;

    private final String couponName;

    private final int issuedQuantity;

    private final int totalQuantity;

    private final int discountRate;

    private final LocalDateTime expiredAt;

    public static CouponListResponse from(Coupon coupon) {
        return CouponListResponse.builder()
                .couponId(coupon.getId())
                .couponName(coupon.getName())
                .discountRate(coupon.getDiscountRate())
                .expiredAt(coupon.getExpiredAt())
                .issuedQuantity(coupon.getIssuedQuantity())
                .totalQuantity(coupon.getTotalQuantity())
                .build();
    }

    public static CouponListResponse from(CouponDocument couponDocument) {
        return CouponListResponse.builder()
                .couponId(couponDocument.getId())
                .couponName(couponDocument.getName())
                .discountRate(couponDocument.getDiscountRate())
                .expiredAt(couponDocument.getExpiredAt())
                .issuedQuantity(couponDocument.getIssuedQuantity())
                .totalQuantity(couponDocument.getTotalQuantity())
                .build();
    }
}
