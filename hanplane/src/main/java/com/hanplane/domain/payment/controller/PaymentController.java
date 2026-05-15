package com.hanplane.domain.payment.controller;

import com.hanplane.domain.payment.dto.PaymentConfirmRequest;
import com.hanplane.domain.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/confirm")
    public ResponseEntity<Void> confirm(@RequestBody PaymentConfirmRequest request) {
        paymentService.confirm(request);
        return ResponseEntity.ok().build();
    }
}