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
    void 쿠폰_발급_성공() throws InterruptedException {
        // given
        Long userId = 1L;
        Long couponId = 1L;

        when(redissonClient.getLock("coupon:lock:" + couponId)).thenReturn(rLock);
        when(rLock.tryLock(30, 3, TimeUnit.SECONDS)).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);

        // when
        couponService.issueCoupon(userId, couponId);

        // then
        verify(couponIssueService).issue(userId, couponId);
        verify(rLock).unlock();
    }

    @Test
    void 락_획득_실패시_예외발생() throws InterruptedException {
        // given
        Long userId = 1L;
        Long couponId = 1L;

        when(redissonClient.getLock("coupon:lock:" + couponId)).thenReturn(rLock);
        when(rLock.tryLock(30, 3, TimeUnit.SECONDS)).thenReturn(false);
        when(rLock.isHeldByCurrentThread()).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> couponService.issueCoupon(userId, couponId))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.LOCK_TRY_FAIL.getMessage());

        verify(couponIssueService, never()).issue(anyLong(), anyLong());
        verify(rLock, never()).unlock();
    }
}