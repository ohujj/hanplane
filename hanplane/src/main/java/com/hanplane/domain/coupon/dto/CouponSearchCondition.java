package com.hanplane.domain.coupon.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDate;

@Getter
@Builder
@Jacksonized
public class CouponSearchCondition {

    private final String name;
    private final Integer discountRate;
    private final LocalDate expiryDate;
}
