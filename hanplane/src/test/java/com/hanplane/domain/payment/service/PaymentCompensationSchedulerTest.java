package com.hanplane.domain.payment.service;

import com.hanplane.global.logging.TraceIdFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doAnswer;

class PaymentCompensationSchedulerTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void cancelRequiredScheduleRunsWithBatchTraceId() {
        // given
        PaymentCompensationService service = mock(PaymentCompensationService.class);
        PaymentCompensationScheduler scheduler = new PaymentCompensationScheduler(service);

        doAnswer(invocation -> {
            assertThat(MDC.get(TraceIdFilter.TRACE_ID)).startsWith("batch-");
            return null;
        }).when(service).retryCancelRequiredPayments();

        // when
        scheduler.retryCancelRequiredPayments();

        // then
        verify(service).retryCancelRequiredPayments();
        assertThat(MDC.get(TraceIdFilter.TRACE_ID)).isNull();
    }

    @Test
    void verifyRequiredScheduleRunsWithBatchTraceId() {
        // given
        PaymentCompensationService service = mock(PaymentCompensationService.class);
        PaymentCompensationScheduler scheduler = new PaymentCompensationScheduler(service);

        doAnswer(invocation -> {
            assertThat(MDC.get(TraceIdFilter.TRACE_ID)).startsWith("batch-");
            return null;
        }).when(service).retryVerifyRequiredPayments();

        // when
        scheduler.retryVerifyRequiredPayments();

        // then
        verify(service).retryVerifyRequiredPayments();
        assertThat(MDC.get(TraceIdFilter.TRACE_ID)).isNull();
    }
}
