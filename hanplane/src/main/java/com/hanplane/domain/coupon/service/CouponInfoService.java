package com.hanplane.domain.coupon.service;

import com.hanplane.domain.coupon.dto.CouponListResponse;
import com.hanplane.domain.coupon.dto.CouponUpdateRequest;
import com.hanplane.domain.coupon.dto.UserCouponResponse;
import com.hanplane.domain.coupon.entity.Coupon;
import com.hanplane.domain.coupon.entity.UserCoupon;
import com.hanplane.domain.coupon.repository.CouponRepository;
import com.hanplane.domain.coupon.repository.UserCouponRepository;
import com.hanplane.global.exception.BusinessException;
import com.hanplane.global.exception.ErrorCode;
import com.hanplane.global.jwt.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponInfoService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    public Page<CouponListResponse> getCouponList(Pageable pageable) {
        return couponRepository.findByDeletedAtIsNull(pageable)
                .map(CouponListResponse :: from);
    }

    public CouponListResponse getCouponDetail(Long couponId) {
        return couponRepository.findById(couponId).map(CouponListResponse::from).orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));
    }

    @Transactional
    public void updateCoupon(Long couponId, CouponUpdateRequest request) {
        Coupon coupon = couponRepository.findById(couponId).orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));
        if(request.getName() != null) {
            coupon.updateName(request.getName());
        }

        if(request.getExpiredAt() != null) {
            coupon.updateExpiredAt(request.getExpiredAt());
        }

        if(request.getDiscountRate() != null) {
            coupon.updateDiscountRate(request.getDiscountRate());
        }

        if(request.getTotalQuantity() != null) {
            coupon.updateTotalQuantity(request.getTotalQuantity());
        }
    }

    @Transactional
    public void deleteCoupon(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId).orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));

        coupon.delete();
    }

    public List<UserCouponResponse> getUserCouponByUserId(Long userId) {
        //return userCouponRepository.findByUserId(userId).stream()
        return userCouponRepository.findByUserIdWithCouponAndUser(userId).stream()
                .map(UserCouponResponse :: from)
                .collect(Collectors.toList());

    }
}
