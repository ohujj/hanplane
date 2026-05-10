package com.hanplane.domain.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderItemRequest {
    @NotNull
    @Min(0)
    private Long productId;

    @NotNull
    @Min(0)
    private Integer quantity;
}
