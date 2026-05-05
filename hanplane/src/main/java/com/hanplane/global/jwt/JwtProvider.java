package com.hanplane.global.jwt;

import com.hanplane.domain.auth.dto.RefreshTokenInfo;
import com.hanplane.domain.auth.entity.RefreshToken;
import com.hanplane.domain.auth.repository.AuthRepository;
import com.hanplane.domain.user.entity.Role;
import com.hanplane.global.exception.BusinessException;
import com.hanplane.global.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.misc.Hash;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class JwtProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private Long expiration;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }


    // 토큰 생성
    public String generateToken(Long userId, Role role) {
        return Jwts.builder()
                .claim("userId", userId)
                .claim("role", role.name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public RefreshTokenInfo generateRefreshToken(Long userId) {


        Date date = new Date(System.currentTimeMillis() + refreshExpiration);

        String refreshToken = Jwts.builder()
                .claim("userId", userId)
                .issuedAt(new Date())
                .expiration(date)
                .signWith(getSigningKey())
                .compact();

        LocalDateTime expiresAt = date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        return new RefreshTokenInfo(refreshToken, expiresAt);
    }

    // 토큰 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.info("jwt토큰이 만료 되었습니다.");
            throw new BusinessException(ErrorCode.EXPIRED_TOKEN);
        } catch (Exception e) {
            log.info("ValidateToken 중 오류 발생 :  " + e.getMessage());
            return false;
        }
    }

    // 토큰에서 userId 및 role 추출
    public UserPrincipal getPrincipal(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Long userId = claims.get("userId", Long.class);
        Role role = Role.valueOf(claims.get("role", String.class));

        return new UserPrincipal(userId, role);
    }



}
