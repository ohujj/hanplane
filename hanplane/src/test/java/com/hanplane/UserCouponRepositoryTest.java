package com.hanplane;

import com.hanplane.domain.coupon.entity.Coupon;
import com.hanplane.domain.coupon.entity.CouponStatus;
import com.hanplane.domain.coupon.entity.UserCoupon;
import com.hanplane.domain.coupon.repository.CouponRepository;
import com.hanplane.domain.coupon.repository.UserCouponRepository;
import com.hanplane.domain.user.entity.User;
import com.hanplane.domain.user.repository.UserRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;


import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class UserCouponRepositoryTest {

    @Autowired
    CouponRepository couponRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    UserCouponRepository userCouponRepository;

    @Test
    void 유저쿠폰_저장_확인() {
        //given
        User user = User.builder()
                .email("test@mail.com")
                .password("1234")
                .build();

        User savedUser = userRepository.save(user);

        Coupon coupon = Coupon.builder()
                .name("테스트_쿠폰_1")
                .discountRate(50)
                .expiredAt(LocalDateTime.now().plusDays(30))
                .totalQuantity(100)
                .build();

        Coupon savedCoupon = couponRepository.save(coupon);

        UserCoupon userCoupon = UserCoupon.builder()
                .user(savedUser)
                .coupon(savedCoupon)
                .issuedAt(LocalDateTime.now())
                .status(CouponStatus.UNUSED)
                .build();
        //when
        UserCoupon save = userCouponRepository.save(userCoupon);

        //then
        assertThat(save.getId()).isNotNull();
        assertThat(save.getUser()).isEqualTo(user);
    }
}

