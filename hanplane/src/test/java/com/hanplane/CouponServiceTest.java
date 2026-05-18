package com.hanplane;

import com.hanplane.domain.coupon.entity.Coupon;
import com.hanplane.domain.coupon.entity.CouponStatus;
import com.hanplane.domain.coupon.entity.UserCoupon;
import com.hanplane.domain.coupon.repository.CouponRepository;
import com.hanplane.domain.coupon.repository.UserCouponRepository;
import com.hanplane.domain.coupon.service.CouponIssueService;
import com.hanplane.domain.coupon.service.CouponService;
import com.hanplane.domain.user.entity.User;
import com.hanplane.domain.user.repository.UserRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RedissonClient;

import java.time.LocalDateTime;
import java.util.Optional;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CouponServiceTest {

    @InjectMocks
    private CouponService couponService;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private CouponIssueService couponIssueService;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserCouponRepository userCouponRepository;


    @Test
    void 쿠폰_발급_성공() {
        // given
        User user = User.builder()
                .email("test@email.com")
                .password("1234")
                .build();

        Coupon coupon = Coupon.builder()
                .name("테스트_쿠폰_1")
                .discountRate(50)
                .expiredAt(LocalDateTime.now().plusDays(30))
                .totalQuantity(100)
                .build();

        // Mock 설정
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
        when(userCouponRepository.existsByUserIdAndCouponId(1L, 1L)).thenReturn(false);

        when(redissonClient.getLock(anyString())).thenReturn(rLock); // getLock → null 대신 Mock 반환
        when(rLock.tryLock(anyLong(), anyLong(), any())).thenReturn(true); // 락 획득 성공으로 설정
        when(rLock.isHeldByCurrentThread()).thenReturn(true); // finally 블록 unlock 조건 통과

        // when - 실제 테스트할 메서드 호출
        couponService.issueCoupon(1L, 1L);

        // THEN
        verify(userCouponRepository, times(1)).save(any(UserCoupon.class));
        assertThat(coupon.getIssuedQuantity()).isEqualTo(1);

    }

}