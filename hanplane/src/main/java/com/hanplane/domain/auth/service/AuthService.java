package com.hanplane.domain.auth.service;

import com.hanplane.domain.auth.dto.*;
import com.hanplane.domain.auth.entity.RefreshToken;
import com.hanplane.domain.auth.repository.AuthRepository;
import com.hanplane.domain.user.entity.Role;
import com.hanplane.domain.user.entity.User;
import com.hanplane.domain.user.repository.UserRepository;
import com.hanplane.global.exception.BusinessException;
import com.hanplane.global.exception.ErrorCode;
import com.hanplane.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final AuthRepository authRepository;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final BCryptPasswordEncoder passwordEncoder;

    public ReissueResponse reissue(ReissueRequest reissueRequest) {
        RefreshToken token = authRepository.findByToken(reissueRequest.getRefreshToken()).orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        boolean validated = jwtProvider.validateToken(token.getToken());
        if(!validated) {
            throw new BusinessException(ErrorCode.JWT_TOKEN_VALIDATE_FAIL);
        }


        User user = token.getUser();

        LocalDateTime now = LocalDateTime.now();
        boolean after = now.isAfter(token.getExpiresAt());
        if(after) {
            throw new BusinessException(ErrorCode.EXPIRED_REFRESH_TOKEN);
        }

        return ReissueResponse.builder()
                .accessToken(jwtProvider.generateToken(user.getId(), user.getRole()))
                .build();
    }

    @Transactional
    public void saveRefreshToken (User user, String refreshToken, LocalDateTime expiresAt) {
        RefreshToken findToken = authRepository.findByUserId(user.getId()).orElse(null);

        if(findToken == null) {
            RefreshToken token = RefreshToken.builder()
                    .user(user)
                    .token(refreshToken)
                    .expiresAt(expiresAt)
                    .build();

            authRepository.save(token);

            return;
        }

        findToken.updateToken(refreshToken, expiresAt);
    }

    public RefreshToken findRefreshToken(User user) {
        return authRepository.findByUserId(user.getId()).orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if(!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_NOT_EQUAL);
        }


        Long userId = user.getId();
        Role role = user.getRole();

        log.info(role + "롤 디버깅");

        RefreshTokenInfo refreshTokenInfo = jwtProvider.generateRefreshToken(userId);

        saveRefreshToken(user, refreshTokenInfo.token(), refreshTokenInfo.expiresAt());


        return LoginResponse.builder()
                .userId(userId)
                .role(role)
                .accessToken(jwtProvider.generateToken(userId, role))
                .refreshToken(refreshTokenInfo.token())
                .build();
    }

}
