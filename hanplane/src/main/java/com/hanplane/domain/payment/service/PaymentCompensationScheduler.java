package com.hanplane.domain.payment.service;

import com.hanplane.global.logging.TraceIdFilter;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PaymentCompensationScheduler {

    private final PaymentCompensationService paymentCompensationService;

    @Scheduled(fixedDelayString = "${payment.compensation.cancel-required-delay-ms:60000}")
    public void retryCancelRequiredPayments() {
        runWithBatchTraceId(paymentCompensationService::retryCancelRequiredPayments);
    }

    @Scheduled(fixedDelayString = "${payment.compensation.verify-required-delay-ms:60000}")
    public void retryVerifyRequiredPayments() {
        runWithBatchTraceId(paymentCompensationService::retryVerifyRequiredPayments);
    }

    private void runWithBatchTraceId(Runnable task) {
        String traceId = "batch-" + UUID.randomUUID();
        try {
            MDC.put(TraceIdFilter.TRACE_ID, traceId);
            task.run();
        } finally {
            MDC.remove(TraceIdFilter.TRACE_ID);
        }
    }
}
