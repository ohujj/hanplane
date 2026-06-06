package com.hanplane.domain.payment.entity;

public enum PayStatus {

    PENDING, PROCESSING,

    SUCCESS, CANCEL, FAIL, REFUNDED,

    ILLEGAL,

    VERIFY_REQUIRED,
    CANCEL_REQUIRED;
}
