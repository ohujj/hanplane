package com.hanplane.domain.coupon.service;

import com.hanplane.domain.coupon.entity.Coupon;
import com.hanplane.domain.coupon.entity.CouponStatus;
import com.hanplane.domain.coupon.entity.UserCoupon;
import com.hanplane.domain.coupon.repository.CouponRepository;
import com.hanplane.domain.coupon.repository.UserCouponRepository;
import com.hanplane.domain.user.entity.User;
import com.hanplane.domain.user.repository.UserRepository;
import com.hanplane.global.exception.BusinessException;
import com.hanplane.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CouponIssueService {

    private final CouponRepository couponRepository;
    private final UserRepository userRepository;
    private final UserCouponRepository userCouponRepository;

    @Transactional
    public void issue(Long userId, Long couponId) {
        Coupon coupon = couponRepository.findById(couponId).orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        boolean isExists = userCouponRepository.existsByUserIdAndCouponId(userId, couponId);
        log.info("isExists = {}", isExists);

        if (isExists) {
            throw new BusinessException(ErrorCode.COUPON_ALREADY_ISSUED);
        }

        int totalQuantity = coupon.getTotalQuantity();
        int issuedQuantity = coupon.getIssuedQuantity();

        if (totalQuantity <= issuedQuantity) {
            throw new BusinessException(ErrorCode.COUPON_SOLD_OUT);
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

    @Transactional
    public void issueWithPessimisticLock(Long userId, Long couponId) {
        Coupon coupon = couponRepository.findByIdWithLock(couponId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        boolean isExists = userCouponRepository.existsByUserIdAndCouponId(userId, couponId);
        log.info("isExists = {}", isExists);

        if (isExists) {
            throw new BusinessException(ErrorCode.COUPON_ALREADY_ISSUED);
        }

        int totalQuantity = coupon.getTotalQuantity();
        int issuedQuantity = coupon.getIssuedQuantity();

        if (totalQuantity <= issuedQuantity) {
            throw new BusinessException(ErrorCode.COUPON_SOLD_OUT);
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
