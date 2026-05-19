package com.hanplane;

import com.hanplane.domain.coupon.service.CouponIssueService;
import com.hanplane.domain.coupon.service.CouponService;
import com.hanplane.global.exception.BusinessException;
import com.hanplane.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @InjectMocks
    private CouponService couponService;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock rLock;

    @Mock
    private CouponIssueService couponIssueService;

    @Test
    void 쿠폰_발급_성공() {
        // given    
        Long userId = 1L;
        Long couponId = 1L;

        // when
        couponService.issueCouponWithPessimisticLock(userId, couponId);

        // then
        verify(couponIssueService).issueWithPessimisticLock(userId, couponId);
    }


}