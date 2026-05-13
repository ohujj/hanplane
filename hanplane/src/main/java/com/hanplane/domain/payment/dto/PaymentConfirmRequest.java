package com.hanplane.domain.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class PaymentConfirmRequest {

    @NotNull
    private final Long orderId;

    @NotNull
    private final String paymentId;

    @NotNull
    private final String txId;
}
