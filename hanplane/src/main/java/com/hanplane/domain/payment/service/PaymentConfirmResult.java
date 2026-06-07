package com.hanplane.domain.payment.service;

import com.hanplane.domain.payment.entity.Payment;

public record PaymentConfirmResult(Payment payment, boolean newlyCreated) {
}