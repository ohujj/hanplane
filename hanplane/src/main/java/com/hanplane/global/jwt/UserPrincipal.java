package com.hanplane.global.jwt;

import com.hanplane.domain.user.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

public record UserPrincipal(Long userId, Role role) {

}

