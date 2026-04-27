package com.hanplane.domain.coupon.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;

@Getter
@Builder
@Jacksonized
public class CouponCreateRequest {

    private final String name;

    private final int discountRate;

    private final int totalQuantity;

    private final LocalDateTime expiredAt;


}
