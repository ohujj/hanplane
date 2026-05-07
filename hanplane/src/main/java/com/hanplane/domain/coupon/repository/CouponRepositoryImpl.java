package com.hanplane.domain.coupon.repository;

import com.hanplane.domain.coupon.dto.CouponSearchCondition;
import com.hanplane.domain.coupon.entity.Coupon;
import com.hanplane.domain.coupon.entity.QCoupon;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


import static com.querydsl.jpa.JPAExpressions.selectFrom;

@Repository
@RequiredArgsConstructor
public class    CouponRepositoryImpl implements CouponRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Coupon> searchCoupons(CouponSearchCondition condition, Pageable pageable) {
        QCoupon coupon = QCoupon.coupon;

        String name = condition.getName();
        Integer discountRate = condition.getDiscountRate();
        LocalDateTime expiryDate = condition.getExpiryDate();

        JPAQuery<Long> count = queryFactory.select(coupon.count())
                .from(coupon)
                .where(name != null ? coupon.name.contains(name) : null,
                        discountRate != null ? coupon.discountRate.eq(discountRate) : null,
                        expiryDate != null ? coupon.expiredAt.after(expiryDate) : null);

        List<Coupon> list = queryFactory.selectFrom(coupon)
                .where(name != null ? coupon.name.contains(name) : null,
                        discountRate != null ? coupon.discountRate.eq(discountRate) : null,
                        expiryDate != null ? coupon.expiredAt.after(expiryDate) : null)
                .fetch();

        return PageableExecutionUtils.getPage(list, pageable, count::fetchOne);
    }
}
