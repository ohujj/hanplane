package com.hanplane.domain.coupon.service;

import com.hanplane.domain.coupon.entity.Coupon;
import com.hanplane.domain.coupon.entity.CouponStatus;
import com.hanplane.domain.coupon.entity.UserCoupon;
import com.hanplane.domain.coupon.repository.CouponRepository;
import com.hanplane.domain.coupon.repository.UserCouponRepository;
import com.hanplane.domain.user.entity.User;
import com.hanplane.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserRepository userRepository;
    private final UserCouponRepository userCouponRepository;


    @Transactional
    public void issueCoupon(Long userId, Long couponId) {

        Coupon coupon = couponRepository.findById(couponId).orElseThrow(() -> new RuntimeException("쿠폰이 없습니다."));
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("사용자가 없습니다."));

        boolean isExists = userCouponRepository.existsByUserIdAndCouponId(userId, couponId);
        log.info("isExists = {}", isExists);

        if(isExists) {
            throw new RuntimeException("이미 발급 된 쿠폰입니다.");
        }

        int totalQuantity = coupon.getTotalQuantity();
        int issuedQuantity = coupon.getIssuedQuantity();

        if(totalQuantity <= issuedQuantity) {
            throw new RuntimeException("정원 초과 되었습니다.");
        }

        coupon.issue();

        UserCoupon userCoupon = UserCoupon.builder()
                .user(user)
                .coupon(coupon)
                .status(CouponStatus.UNUSED)
                .issuedAt(LocalDateTime.now())
                .build();

        UserCoupon save = userCouponRepository.save(userCoupon);

    }

}
