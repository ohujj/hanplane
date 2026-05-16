package com.hanplane.domain.payment.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Getter
@Builder
@Jacksonized
public class RefundRequest {

    private Long paymentId;

    private List<Long> orderItemIds;

}
