package com.hanplane.domain.payment.service;

import com.hanplane.domain.order.repository.OrderRepository;
import com.hanplane.domain.payment.dto.PaymentConfirmRequest;
import com.hanplane.domain.payment.entity.Payment;
import com.hanplane.global.exception.BusinessException;
import com.hanplane.global.exception.ErrorCode;
import io.portone.sdk.server.PortOneClient;
import io.portone.sdk.server.payment.PaidPayment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentConfirmService paymentConfirmService;
    private final PortOneClient portOneClient;
    private final PaymentAfterService paymentAfterService;

    public void confirm(Long userId, PaymentConfirmRequest request, String idempotencyKey) {
        PaymentConfirmResult result = paymentConfirmService.confirmOrder(userId, request, idempotencyKey);

        // 기존 Payment 재사용인 경우(idempotent retry): PG 재호출 없이 정상 종료
        if (!result.newlyCreated()) {
            log.info("멱등성 키 중복 요청 - orderId={}, idempotencyKey 재사용으로 payProcess 스킵",
                    request.getOrderId());
            return;
        }

        // 새로 생성된 Payment에 대해서만 PG 결제 진행
        payProcess(request, result.payment());
    }

    // 결제 실제 진행되는 프로세스
    public void payProcess(PaymentConfirmRequest request, Payment payment) {
        io.portone.sdk.server.payment.Payment pgPayment;
        PaidPayment paidPayment = null;

        try {
            pgPayment = portOneClient.getPayment().getPayment(request.getPaymentId()).get();

            // return 된 인스턴스가 Paid 객체인지 확인하기
            if (!(pgPayment instanceof PaidPayment)) {
                throw new BusinessException(ErrorCode.PG_PAYMENT_NOT_PAID);
            }

            paidPayment = (PaidPayment) pgPayment;

            // 가격 조작되진 않았는지 validation
            long pgAmount = paidPayment.getAmount().getTotal();
            if (pgAmount != payment.getAmount()) {
                // 조작된 사용자라 판단하여 pg 취소 혹은 후처리 로직 메서드
                paymentAfterService.illegalRequestProcess(request);
                throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
            }

        } catch (BusinessException e) {
            if (e.getErrorCode() != ErrorCode.PAYMENT_AMOUNT_MISMATCH) {
                paymentAfterService.payExceptionProcess(request);
            }
            throw e;
        } catch (Exception e) {
            paymentAfterService.payExceptionProcess(request);
            throw new BusinessException(ErrorCode.PG_CALL_FAILED);
        }
        paymentAfterService.payAfterProcess(request, paidPayment);
    }
}