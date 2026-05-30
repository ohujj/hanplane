package com.hanplane.domain.payment.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Getter
@Builder
@Jacksonized
public class RefundRequest {

    @NotNull(message = "결제 ID는 필수입니다.")
    private Long paymentId;

    @NotEmpty(message = "환불할 주문상품은 1개 이상이어야 합니다.")
    private List<
            @NotNull(message = "주문상품 ID는 필수입니다.")
            @Positive(message = "주문상품 ID는 1 이상이어야 합니다.")Long> orderItemIds;

}
