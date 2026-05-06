package com.hanplane.domain.coupon.repository;

import com.hanplane.domain.coupon.dto.CouponSearchCondition;
import com.hanplane.domain.coupon.entity.Coupon;
import com.hanplane.domain.coupon.entity.QCoupon;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;


import static com.querydsl.jpa.JPAExpressions.selectFrom;

@Repository
@RequiredArgsConstructor
public class CouponRepositoryImpl implements CouponRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Coupon> searchCoupons(CouponSearchCondition condition) {
        QCoupon coupon = QCoupon.coupon;

        return null;
    }
}
