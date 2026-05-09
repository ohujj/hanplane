package com.hanplane.domain.product.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;

@Getter
@Builder
@Jacksonized
public class ProductCreateRequest {

    @NotBlank
    private final String name;

    @NotNull
    @Min(0)
    private final Integer price;

    @NotNull
    @Min(0)
    private final Integer totalQuantity;

    @NotNull
    @Min(0)
    private final Integer availQuantity;

    @NotNull
    @Future
    private final LocalDateTime expiredAt;

}
