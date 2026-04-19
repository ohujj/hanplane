package com.hanplane.domain.coupon.repository;

import com.hanplane.domain.coupon.entity.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    boolean existsByUserIdAndCouponId(Long userId, Long couponId);

    List<UserCoupon> findByUserId(Long userId);

    @Query("SELECT uc FROM UserCoupon uc JOIN FETCH uc.coupon JOIN FETCH uc.user WHERE uc.user.id = :userId")
    List<UserCoupon> findByUserIdWithCouponAndUser(@Param("userId") Long userId);
}

