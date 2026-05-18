package com.hanplane.domain.payment.controller;

import com.hanplane.domain.payment.dto.PaymentConfirmRequest;
import com.hanplane.domain.payment.dto.RefundRequest;
import com.hanplane.domain.payment.service.PaymentService;
import com.hanplane.domain.payment.service.RefundService;
import com.hanplane.global.jwt.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final RefundService refundService;

    @PostMapping("/confirm")
    public ResponseEntity<Void> confirm(@RequestBody @Valid PaymentConfirmRequest request,
                                        @AuthenticationPrincipal UserPrincipal userPrincipal) {
        paymentService.confirm(userPrincipal.userId(), request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refund")
    public ResponseEntity<Void> refund(@RequestBody @Valid RefundRequest request,
                                       @AuthenticationPrincipal UserPrincipal userPrincipal) {
        refundService.refundProcess(userPrincipal.userId(), request);
        return ResponseEntity.ok().build();
    }
}