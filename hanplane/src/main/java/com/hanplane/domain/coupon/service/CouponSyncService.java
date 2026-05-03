package com.hanplane.domain.coupon.service;

import com.hanplane.domain.coupon.CouponDocument;
import com.hanplane.domain.coupon.entity.Coupon;
import com.hanplane.domain.coupon.repository.CouponElasticsearchRepository;
import com.hanplane.domain.coupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile("local")
public class CouponSyncService {

    private final CouponRepository couponRepository;

    @Autowired(required = false)
    private final CouponElasticsearchRepository couponElasticsearchRepository;

    public void syncAll() {
        List<Coupon> coupons = couponRepository.findByDeletedAtIsNull();

        int batchSize = 1000;
        for(int i=0; i<coupons.size(); i+= batchSize) {
            int end = Math.min(i + batchSize, coupons.size());

            List<CouponDocument> documents = coupons.subList(i, end).stream()
                    .map(CouponDocument :: from)
                    .collect(Collectors.toList());
            couponElasticsearchRepository.saveAll(documents);
            log.info("ES 동기화 진행중: {}/{}", end, coupons.size());
        }

    }

    public void syncOne(Coupon coupon) {
        couponElasticsearchRepository.save(CouponDocument.from(coupon));
    }

    public void deleteOne(Long couponId) {
        couponElasticsearchRepository.deleteById(couponId);
    }
}
