package com.hanplane.domain.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderItemRequest {
    @NotNull(message = "상품 ID는 필수입니다.")
    @Positive(message = "상품 ID는 1 이상이어야 합니다.")
    private Long productId;

    @NotNull(message = "수량은 필수입니다.")
    @Positive(message = "수량은 1 이상이어야 합니다.")
    private Integer quantity;
}
