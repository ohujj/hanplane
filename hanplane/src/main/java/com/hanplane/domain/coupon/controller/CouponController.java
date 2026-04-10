package com.hanplane.domain.coupon.controller;


import com.hanplane.domain.coupon.dto.CouponIssueRequest;
import com.hanplane.domain.coupon.entity.Coupon;
import com.hanplane.domain.coupon.service.CouponIssueService;
import com.hanplane.domain.coupon.service.CouponService;
import com.hanplane.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/coupons")
public class CouponController {

    private final CouponService couponService;


    @PostMapping("/{couponId}/issue")
    public ResponseEntity<ApiResponse<Void>> issueCoupon(@PathVariable("couponId") Long couponId, @RequestBody CouponIssueRequest request) {
        couponService.issueCoupon(request.getUserId(), couponId);

        return ResponseEntity.ok(ApiResponse.success());
    }
}
