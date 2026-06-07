package com.hanplane.domain.payment.controller;

import com.hanplane.domain.payment.dto.PaymentConfirmRequest;
import com.hanplane.domain.payment.dto.RefundRequest;
import com.hanplane.domain.payment.service.PaymentService;
import com.hanplane.domain.payment.service.RefundService;
import com.hanplane.global.exception.BusinessException;
import com.hanplane.global.exception.ErrorCode;
import com.hanplane.global.jwt.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final RefundService refundService;

    @PostMapping("/confirm")
    public ResponseEntity<Void> confirm(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody @Valid PaymentConfirmRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        // required=false로 null 포함 blank 모두 거부
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER);
        }

        paymentService.confirm(userPrincipal.userId(), request, idempotencyKey);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refund")
    public ResponseEntity<Void> refund(@RequestBody @Valid RefundRequest request,
                                       @AuthenticationPrincipal UserPrincipal userPrincipal) {
        refundService.refundProcess(userPrincipal.userId(), request);
        return ResponseEntity.ok().build();
    }
}