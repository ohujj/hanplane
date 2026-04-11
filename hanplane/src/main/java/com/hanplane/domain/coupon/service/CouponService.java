package com.hanplane.domain.coupon.service;

import com.hanplane.domain.coupon.dto.CouponListResponse;
import com.hanplane.global.exception.BusinessException;
import com.hanplane.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class CouponService {

    private final CouponIssueService couponIssueService;

    private final RedissonClient redissonClient;

    private final String couponLockKey = "coupon:lock:";


    public void issueCoupon(Long userId, Long couponId) {

        RLock lock = redissonClient.getLock(couponLockKey + couponId);

        try {
            boolean hasLock = lock.tryLock(30, 3, TimeUnit.SECONDS);
            if (!hasLock) {
                throw new BusinessException(ErrorCode.LOCK_TRY_FAIL);
            }

            couponIssueService.issue(userId, couponId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if(lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

    }

    public void issueCouponWithPessimisticLock(Long userId, Long couponId) {
        couponIssueService.issueWithPessimisticLock(userId, couponId);
    }

}
