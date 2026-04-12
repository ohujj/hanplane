package com.hanplane.domain.user.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class LoginResponse {

    private final Long userId;

    private final String accessToken;

}
