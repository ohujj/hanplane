package com.hanplane.domain.order.controller;

import com.hanplane.domain.order.dto.OrderCreateRequest;
import com.hanplane.domain.order.service.OrderService;
import com.hanplane.global.jwt.UserPrincipal;
import com.hanplane.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;

    @PostMapping()
    public ResponseEntity<ApiResponse<Void>> createOrder(
            @RequestBody @Valid OrderCreateRequest orderCreateRequest,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
            orderService.createOrder(orderCreateRequest, userPrincipal.userId());

        }

    }
