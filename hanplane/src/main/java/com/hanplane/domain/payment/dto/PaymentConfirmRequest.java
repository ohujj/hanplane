package com.hanplane.domain.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class PaymentConfirmRequest {

    @NotNull(message = "주문 ID는 필수입니다.")
    @Positive(message = "주문 ID는 1 이상이어야 합니다.")
    private final Long orderId;

    @NotBlank(message = "paymentId는 필수입니다.")
    private final String paymentId;

    @NotBlank(message = "txId는 필수입니다.")
    private final String txId;
}