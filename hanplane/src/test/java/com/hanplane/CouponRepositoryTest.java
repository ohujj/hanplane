package com.hanplane;

import com.hanplane.domain.coupon.entity.Coupon;
import com.hanplane.domain.coupon.repository.CouponRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;


@DataJpaTest
public class CouponRepositoryTest {

    @Autowired
    private CouponRepository couponRepository;

    @Test
    void 쿠폰_저장_테스트() {

        //given
        Coupon coupon = Coupon.builder()
                .name("테스트 쿠폰_1")
                .discountRate(10)
                .expiredAt(LocalDateTime.now().plusDays(30))
                .totalQuantity(50)
                .build();

        //when
        Coupon save = couponRepository.save(coupon);

        //then
        assertThat(save.getId()).isNotNull();
        assertThat(save.getName()).isEqualTo("테스트 쿠폰_1");
    }
}
