package com.hanplane;

import com.hanplane.domain.coupon.entity.Coupon;
import com.hanplane.domain.coupon.repository.CouponRepository;
import com.hanplane.domain.coupon.repository.UserCouponRepository;
import com.hanplane.domain.coupon.service.CouponService;
import com.hanplane.domain.user.entity.User;
import com.hanplane.domain.user.repository.UserRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@SpringBootTest
public class ConcurrencyTest {

    @Autowired
    private CouponService couponService;
    @Autowired
    private CouponRepository couponRepository;
    @Autowired
    private UserCouponRepository userCouponRepository;
    @Autowired
    private UserRepository userRepository;

    @Test
    void 동시에_100명이_쿠폰_발급_요청() throws InterruptedException {
        // given
        Coupon coupon = Coupon.builder()
                .name("100명 제한 쿠폰")
                .totalQuantity(100)
                .expiredAt(LocalDateTime.now().plusDays(7))
                .discountRate(10)
                .build();

        Coupon savedCoupon = couponRepository.save(coupon);

        List<User> userList = new ArrayList<>();
        for(int i=1; i<=100; i++) {
            User user = User.builder()
                    .email("user" + i + "@test.com")
                    .password("testUser" + i)
                    .build();
            User savedUser = userRepository.save(user);
            userList.add(savedUser);
        }

        // when
        long start = System.currentTimeMillis();

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(100);

        for(User user : userList) {
            Long userId = user.getId();

            new Thread(() -> {
                try {
                    startLatch.await();
                    couponService.issueCoupon(userId, savedCoupon.getId());
//                    couponService.issueCouponWithPessimisticLock(userId, savedCoupon.getId());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();

        // 100개 스레드 전부 끝날 때까지 대기
        endLatch.await();

        long end = System.currentTimeMillis();
        System.out.println("Redis 분산락 사용 소요 시간: " + (end - start) + "ms");

        // then
        Coupon findCoupon = couponRepository.findById(savedCoupon.getId()).orElseThrow();
        int issuedQuantity = findCoupon.getIssuedQuantity();

        System.out.println("발급 된 수량 : " + issuedQuantity);

        Assertions.assertThat(issuedQuantity).isEqualTo(100);

    }
}
