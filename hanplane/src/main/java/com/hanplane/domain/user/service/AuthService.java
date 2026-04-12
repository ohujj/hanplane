package com.hanplane.domain.user.service;

import com.hanplane.domain.user.dto.LoginRequest;
import com.hanplane.domain.user.dto.LoginResponse;
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

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final BCryptPasswordEncoder passwordEncoder;

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if(!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_NOT_EQUAL);
        }


        Long userId = user.getId();

        return LoginResponse.builder()
                .userId(userId)
                .accessToken(jwtProvider.generateToken(userId))
                .build();
    }

}
