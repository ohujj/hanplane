package com.hanplane.domain.auth.dto;

import java.time.LocalDateTime;

public record RefreshTokenInfo(String token, LocalDateTime expiresAt) {
}
