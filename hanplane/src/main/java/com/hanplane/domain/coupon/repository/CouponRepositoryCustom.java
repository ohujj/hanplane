package com.hanplane.domain.coupon.repository;


import com.hanplane.domain.coupon.dto.CouponSearchCondition;
import com.hanplane.domain.coupon.entity.Coupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface CouponRepositoryCustom {

    Page<Coupon> searchCoupons(CouponSearchCondition condition, Pageable pageable);
}
