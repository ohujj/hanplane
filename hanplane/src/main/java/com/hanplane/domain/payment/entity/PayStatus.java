package com.hanplane.domain.payment.entity;

public enum PayStatus {

    PENDING, PROCESSING,
    SUCCESS, CANCEL, FAIL,
    REFUNDED, ILLEGAL;
}
