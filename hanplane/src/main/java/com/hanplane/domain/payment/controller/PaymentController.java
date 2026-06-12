package com.hanplane.domain.payment.controller;

import com.hanplane.domain.payment.dto.PaymentConfirmRequest;
import com.hanplane.domain.payment.dto.PaymentManualReviewResponse;
import com.hanplane.domain.payment.dto.RefundRequest;
import com.hanplane.domain.payment.entity.PayStatus;
import com.hanplane.domain.payment.service.PaymentManualReviewService;
import com.hanplane.domain.payment.service.PaymentService;
import com.hanplane.domain.payment.service.RefundService;
import com.hanplane.domain.user.entity.Role;
import com.hanplane.global.exception.BusinessException;
import com.hanplane.global.exception.ErrorCode;
import com.hanplane.global.jwt.UserPrincipal;
import com.hanplane.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final RefundService refundService;
    private final PaymentManualReviewService paymentManualReviewService;

    @PostMapping("/confirm")
    public ResponseEntity<Void> confirm(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody @Valid PaymentConfirmRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        // Reject both null and blank Idempotency-Key values.
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

    @GetMapping("/manual-reviews")
    public ResponseEntity<ApiResponse<Page<PaymentManualReviewResponse>>> getManualReviewPayments(
            @RequestParam(required = false) PayStatus payStatus,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        if (userPrincipal == null || userPrincipal.role() != Role.ADMIN) {
            throw new BusinessException(ErrorCode.ILLEGAL_REQUEST_PARAMETER);
        }

        Page<PaymentManualReviewResponse> response = paymentManualReviewService.getManualReviewPayments(payStatus, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
