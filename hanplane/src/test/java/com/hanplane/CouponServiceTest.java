package com.hanplane;

import com.hanplane.domain.coupon.service.CouponIssueService;
import com.hanplane.domain.coupon.service.CouponService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @InjectMocks
    private CouponService couponService;

    @Mock
    private CouponIssueService couponIssueService;

    @Test
    void 쿠폰_발급_성공() {
        Long userId = 1L;
        Long couponId = 1L;

        couponService.issueCoupon(userId, couponId);

        verify(couponIssueService).issueWithPessimisticLock(userId, couponId);
    }


}