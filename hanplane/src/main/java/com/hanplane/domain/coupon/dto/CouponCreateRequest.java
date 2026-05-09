package com.hanplane.domain.coupon.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;

@Getter
@Builder
@Jacksonized
public class CouponCreateRequest {

    @NotBlank
    private final String name;

    @NotNull
    @Min(0)
    @Max(100)
    private final Integer discountRate;

    @NotNull
    @Min(0)
    private final Integer totalQuantity;

    @NotNull
    @Future
    private final LocalDateTime expiredAt;


}
