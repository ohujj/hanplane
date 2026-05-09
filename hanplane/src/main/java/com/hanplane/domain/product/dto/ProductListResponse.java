package com.hanplane.domain.product.dto;

import com.hanplane.domain.product.entity.Product;
import com.hanplane.global.entity.BaseEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;

@Getter
@Builder
@Jacksonized

public class ProductListResponse {

    private final Long id;

    private final String name;

    private final int availQuantity;

    private final int totalQuantity;

    private final int price;

    private final LocalDateTime expiredAt;

    public static ProductListResponse from(Product product) {
        return ProductListResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .availQuantity(product.getAvailQuantity())
                .totalQuantity(product.getTotalQuantity())
                .price(product.getPrice())
                .expiredAt(product.getExpiredAt())
                .build();
    }

}
