package com.hanplane.domain.user.controller;

import com.hanplane.domain.user.dto.LoginRequest;
import com.hanplane.domain.user.dto.LoginResponse;
import com.hanplane.domain.user.service.AuthService;
import com.hanplane.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {
        LoginResponse login = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(login));
    }
}
