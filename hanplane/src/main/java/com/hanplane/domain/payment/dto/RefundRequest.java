package com.hanplane.domain.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Getter
@Builder
@Jacksonized
public class RefundRequest {

    @NotNull
    private Long paymentId;

    @NotNull
    private List<Long> orderItemIds;

}
