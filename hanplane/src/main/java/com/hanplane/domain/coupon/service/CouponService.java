package com.hanplane.domain.coupon.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CouponService {

    private final CouponIssueService couponIssueService;

    public void issueCoupon(Long userId, Long couponId) {
        couponIssueService.issueWithPessimisticLock(userId, couponId);
    }
}