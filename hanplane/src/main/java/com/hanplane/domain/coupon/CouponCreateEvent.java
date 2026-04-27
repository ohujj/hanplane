package com.hanplane.domain.coupon;

import com.hanplane.domain.coupon.entity.Coupon;
import lombok.AllArgsConstructor;
import lombok.Getter;

public record CouponCreateEvent(Coupon coupon) {

}
