package com.hanplane.domain.payment.service;

import com.hanplane.domain.payment.dto.PaymentManualReviewResponse;
import com.hanplane.domain.payment.entity.PayStatus;
import com.hanplane.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentManualReviewService {

    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public Page<PaymentManualReviewResponse> getManualReviewPayments(PayStatus payStatus, Pageable pageable) {
        return paymentRepository.findManualReviewPayments(payStatus, pageable)
                .map(PaymentManualReviewResponse::from);
    }
}
