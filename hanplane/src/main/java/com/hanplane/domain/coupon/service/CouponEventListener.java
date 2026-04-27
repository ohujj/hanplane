package com.hanplane.domain.coupon.service;

import com.hanplane.domain.coupon.CouponCreateEvent;
import com.hanplane.domain.coupon.dto.CouponCreateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class CouponEventListener {

    private final CouponSyncService couponSyncService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCouponCreated(CouponCreateEvent event) {
        couponSyncService.syncOne(event.coupon());
    }


}
