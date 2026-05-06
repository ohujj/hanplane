package com.hanplane.domain.coupon.repository;


import com.hanplane.domain.coupon.dto.CouponSearchCondition;
import com.hanplane.domain.coupon.entity.Coupon;

import java.util.List;

public interface CouponRepositoryCustom {

    List<Coupon> searchCoupons(CouponSearchCondition condition);
}
