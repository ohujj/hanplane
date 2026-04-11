package com.hanplane.domain.coupon.service;

import com.hanplane.domain.coupon.dto.CouponListResponse;
import com.hanplane.domain.coupon.dto.CouponUpdateRequest;
import com.hanplane.domain.coupon.entity.Coupon;
import com.hanplane.domain.coupon.repository.CouponRepository;
import com.hanplane.global.exception.BusinessException;
import com.hanplane.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponInfoService {

    private final CouponRepository couponRepository;

    public List<CouponListResponse> getCouponList() {
        return couponRepository.findAll().stream()
                .map(CouponListResponse :: from)
                .collect(Collectors.toList());
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

}
