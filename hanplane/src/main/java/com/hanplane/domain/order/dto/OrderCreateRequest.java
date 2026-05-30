package com.hanplane.domain.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Getter
@Builder
@Jacksonized
public class OrderCreateRequest {

    @NotEmpty(message = "주문 상품은 1개 이상이어야 합니다.")
    @Valid
    private List<OrderItemRequest> orderItems;

    private Long couponId;

}
