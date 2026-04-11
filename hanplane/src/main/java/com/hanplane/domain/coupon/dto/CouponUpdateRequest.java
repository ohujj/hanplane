package com.hanplane.domain.coupon.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;

@Getter
@Builder
@Jacksonized
public class CouponUpdateRequest {

    private final String name;
    private final Integer totalQuantity;
    private final Integer discountRate;
    private final LocalDateTime expiredAt;
}
