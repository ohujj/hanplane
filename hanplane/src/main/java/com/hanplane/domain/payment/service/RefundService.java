package com.hanplane.domain.payment.service;

import com.hanplane.domain.payment.dto.RefundRequest;
import com.hanplane.domain.payment.entity.Payment;
import com.hanplane.domain.payment.entity.Refund;
import com.hanplane.domain.payment.entity.RefundStatus;
import com.hanplane.domain.payment.repository.RefundRepository;
import com.hanplane.global.exception.BusinessException;
import com.hanplane.global.exception.ErrorCode;
import io.portone.sdk.server.PortOneClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundService {

    private final RefundRepository refundRepository;
    private final PortOneClient portOneClient;
    private final RefundSaveService refundSaveService;

    public void refundProcess(Long userId, RefundRequest refundRequest) {
        Refund refund = refundSaveService.refundSaveProcess(userId, refundRequest);

        Payment payment = refund.getPayment();
        int totalPrice = refund.getAmount();

        //PG 환불 요청
        try {
            portOneClient.getPayment().cancelPayment(
                    payment.getPgPaymentId(),
                    (long) totalPrice,
                    null,
                    null,
                    "사용자 환불 요청",
                    null,
                    null,
                    null,
                    null
            ).get();
        } catch (Exception e) {
            refund.updateRefundStatus(RefundStatus.FAIL);
            refundRepository.save(refund);
            throw new BusinessException(ErrorCode.PG_CALL_FAILED);
        }
        //PG 환불 요청 끝

        refundSaveService.refundAfterProcess(refund.getId(), refundRequest);
    }
}
