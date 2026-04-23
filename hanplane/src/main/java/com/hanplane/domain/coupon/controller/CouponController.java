package com.hanplane.domain.coupon.controller;


import com.hanplane.domain.coupon.dto.CouponUpdateRequest;
import com.hanplane.domain.coupon.dto.UserCouponResponse;
import com.hanplane.domain.coupon.entity.UserCoupon;
import com.hanplane.domain.coupon.service.CouponInfoService;
import com.hanplane.domain.coupon.dto.CouponListResponse;
import com.hanplane.domain.coupon.service.CouponService;
import com.hanplane.global.jwt.UserPrincipal;
import com.hanplane.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/coupons")
@Slf4j
public class CouponController {

    private final CouponService couponService;

    private final CouponInfoService couponInfoService;

    @PostMapping("/{couponId}/issue")
    public ResponseEntity<ApiResponse<Void>> issueCoupon(@PathVariable("couponId") Long couponId, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        couponService.issueCoupon(userPrincipal.userId(), couponId);

        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping()
    public ResponseEntity<ApiResponse<Page<CouponListResponse>>> getCouponList(Pageable pageable) {
        Page<CouponListResponse> couponList = couponInfoService.getCouponList(pageable);

        return ResponseEntity.ok(ApiResponse.success(couponList));
    }

    @GetMapping("/{couponId}")
    public ResponseEntity<ApiResponse<CouponListResponse>> getCouponDetail(@PathVariable("couponId") Long couponId) {
        CouponListResponse couponDetail = couponInfoService.getCouponDetail(couponId);

        return ResponseEntity.ok(ApiResponse.success(couponDetail));
    }

    @PatchMapping("/{couponId}")
    public ResponseEntity<ApiResponse<Void>> updateCoupon(@PathVariable("couponId") Long couponId, @RequestBody CouponUpdateRequest couponUpdateRequest, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        couponInfoService.updateCoupon(couponId, couponUpdateRequest);

        return ResponseEntity.ok(ApiResponse.success());
    }

    @DeleteMapping("/{couponId}")
    public ResponseEntity<ApiResponse<Void>> deleteCoupon(@PathVariable("couponId") Long couponId) {
        couponInfoService.deleteCoupon(couponId);

        return ResponseEntity.ok(ApiResponse.success());
    }
}
