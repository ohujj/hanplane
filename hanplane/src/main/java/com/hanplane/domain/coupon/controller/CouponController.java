package com.hanplane.domain.coupon.controller;


import com.hanplane.domain.coupon.dto.CouponUpdateRequest;
import com.hanplane.domain.coupon.service.CouponInfoService;
import com.hanplane.domain.coupon.dto.CouponIssueRequest;
import com.hanplane.domain.coupon.dto.CouponListResponse;
import com.hanplane.domain.coupon.service.CouponService;
import com.hanplane.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public ResponseEntity<ApiResponse<Void>> issueCoupon(@PathVariable("couponId") Long couponId,  @AuthenticationPrincipal Long userId) {

        log.info(userId + " 디버깅 ");

        couponService.issueCoupon(userId, couponId);

        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping()
    public ResponseEntity<ApiResponse<List<CouponListResponse>>> getCouponList() {
        List<CouponListResponse> couponList = couponInfoService.getCouponList();

        return ResponseEntity.ok(ApiResponse.success(couponList));
    }

    @GetMapping("/{couponId}")
    public ResponseEntity<ApiResponse<CouponListResponse>> getCouponDetail(@PathVariable("couponId") Long couponId) {
        CouponListResponse couponDetail = couponInfoService.getCouponDetail(couponId);

        return ResponseEntity.ok(ApiResponse.success(couponDetail));
    }

    @PatchMapping("/{couponId}")
    public ResponseEntity<ApiResponse<Void>> updateCoupon(@PathVariable("couponId") Long couponId, @RequestBody CouponUpdateRequest couponUpdateRequest) {
        couponInfoService.updateCoupon(couponId, couponUpdateRequest);

        return ResponseEntity.ok(ApiResponse.success());
    }


}
