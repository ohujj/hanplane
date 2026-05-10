package com.hanplane.domain.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Getter
@Builder
@Jacksonized
public class OrderCreateRequest {

    @Valid
    private List<OrderItemRequest> orderItems;

    private Long couponId;

}
