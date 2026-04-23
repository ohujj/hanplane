package com.hanplane.domain.coupon.controller;

import com.hanplane.domain.coupon.dto.UserCouponResponse;
import com.hanplane.domain.coupon.service.CouponInfoService;
import com.hanplane.domain.coupon.service.CouponService;
import com.hanplane.global.jwt.UserPrincipal;
import com.hanplane.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/user-coupons")
public class UserCouponController {

    private final CouponInfoService couponInfoService;

    @GetMapping("")
    public ResponseEntity<ApiResponse<List<UserCouponResponse>>> getUserCouponByUserId(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<UserCouponResponse> userCouponByUserId = couponInfoService.getUserCouponByUserId(userPrincipal.userId());

        return ResponseEntity.ok(ApiResponse.success(userCouponByUserId));
    }
}
