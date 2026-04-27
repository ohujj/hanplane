package com.hanplane.domain.coupon.service;

import com.hanplane.domain.coupon.CouponDocument;
import com.hanplane.domain.coupon.entity.Coupon;
import com.hanplane.domain.coupon.repository.CouponElasticsearchRepository;
import com.hanplane.domain.coupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponSyncService {

    private final CouponRepository couponRepository;
    private final CouponElasticsearchRepository couponElasticsearchRepository;

    public void syncAll() {
        List<Coupon> coupons = couponRepository.findByDeletedAtIsNull();
        List<CouponDocument> documents = coupons.stream()
                        .map(CouponDocument :: from)
                        .collect(Collectors.toList());
        couponElasticsearchRepository.saveAll(documents);
    }

    public void syncOne(Coupon coupon) {
        couponElasticsearchRepository.save(CouponDocument.from(coupon));
    }
}
