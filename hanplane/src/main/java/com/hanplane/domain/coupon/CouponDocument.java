package com.hanplane.domain.coupon;

import com.hanplane.domain.coupon.entity.Coupon;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

@Document(indexName = "coupon")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CouponDocument {

    @Id
    private Long id;

    private String name;
    private int discountRate;
    private int totalQuantity;
    private int issuedQuantity;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime expiredAt;

    public static CouponDocument from(Coupon coupon) {
        return CouponDocument.builder()
                .id(coupon.getId())
                .name(coupon.getName())
                .discountRate(coupon.getDiscountRate())
                .totalQuantity(coupon.getTotalQuantity())
                .issuedQuantity(coupon.getIssuedQuantity())
                .expiredAt(coupon.getExpiredAt())
                .build();
    }

}
