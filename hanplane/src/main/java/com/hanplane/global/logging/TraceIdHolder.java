package com.hanplane.global.logging;

import org.slf4j.MDC;

public final class TraceIdHolder {

    private TraceIdHolder() {
    }

    public static String getTraceId() {
        return MDC.get(TraceIdFilter.TRACE_ID);
    }
}
