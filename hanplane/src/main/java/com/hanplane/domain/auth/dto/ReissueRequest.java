package com.hanplane.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class ReissueRequest {

    private final String refreshToken;

}
