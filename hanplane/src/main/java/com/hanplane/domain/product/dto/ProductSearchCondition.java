package com.hanplane.domain.product.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductSearchCondition {
    private String name;
    private Integer price;
    private LocalDateTime expiredAt;
}
