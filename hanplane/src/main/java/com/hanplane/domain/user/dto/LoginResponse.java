package com.hanplane.domain.user.dto;

import com.hanplane.domain.user.entity.Role;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class LoginResponse {

    private final Long userId;

    private final String accessToken;

    private final Role role;

}
