package com.hanplane.domain.product.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;

@Getter
@Builder
@Jacksonized
public class ProductUpdateRequest {


    private final String name;

    @Future
    private final LocalDateTime expiredAt;

    @Min(0)
    private final Integer totalQuantity;

    @Min(0)
    private final Integer availQuantity;

    @Min(0)
    private final Integer price;

}
