package com.hanplane.domain.payment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentCompensationScheduler {

    private final PaymentCompensationService paymentCompensationService;

    @Scheduled(fixedDelayString = "${payment.compensation.cancel-required-delay-ms:60000}")
    public void retryCancelRequiredPayments() {
        paymentCompensationService.retryCancelRequiredPayments();
    }

    @Scheduled(fixedDelayString = "${payment.compensation.verify-required-delay-ms:60000}")
    public void retryVerifyRequiredPayments() {
        paymentCompensationService.retryVerifyRequiredPayments();
    }
}
