package com.hanplane.domain.coupon.controller;


import com.hanplane.domain.coupon.dto.*;
import com.hanplane.domain.coupon.entity.UserCoupon;
import com.hanplane.domain.coupon.service.CouponInfoService;
import com.hanplane.domain.coupon.service.CouponService;
import com.hanplane.domain.coupon.service.CouponSyncService;
import com.hanplane.global.exception.BusinessException;
import com.hanplane.global.exception.ErrorCode;
import com.hanplane.global.jwt.UserPrincipal;
import com.hanplane.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired(required = false)
    CouponSyncService couponSyncService;

    private final CouponInfoService couponInfoService;

    @PostMapping("/{couponId}/issue")
    public ResponseEntity<ApiResponse<Void>> issueCoupon(@PathVariable("couponId") Long couponId, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        couponService.issueCoupon(userPrincipal.userId(), couponId);

        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<CouponListResponse>>> searchCoupon(@ModelAttribute CouponSearchCondition condition, Pageable pageable) {
        Page<CouponListResponse> couponListResponsePage = couponInfoService.elasticsearchCoupon(condition, pageable);

        return ResponseEntity.ok(ApiResponse.success(couponListResponsePage));
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

    @PostMapping()
    public ResponseEntity<ApiResponse<Void>> createCoupon(@RequestBody CouponCreateRequest couponCreateRequest) {
        couponInfoService.createCoupon(couponCreateRequest);

        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<Void>> syncElasticSearchCoupon() {
        if(couponSyncService == null) {
            throw new BusinessException(ErrorCode.ACTIVE_PROFILE_LOCAL);
        }

        couponSyncService.syncAll();

        return ResponseEntity.ok(ApiResponse.success());
    }
}
