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

        if (!result.newlyCreated()) {
            log.info("Payment confirm idempotent retry skipped. orderId={}, pgPaymentId={}, idempotencyKeyPrefix={}",
                    request.getOrderId(), request.getPaymentId(), idempotencyKeyPrefix(idempotencyKey));
            return;
        }

        log.info("Payment confirm PG lookup started. orderId={}, pgPaymentId={}, idempotencyKeyPrefix={}",
                request.getOrderId(), request.getPaymentId(), idempotencyKeyPrefix(idempotencyKey));
        payProcess(request, result.payment());
    }

    public void payProcess(PaymentConfirmRequest request, Payment payment) {
        io.portone.sdk.server.payment.Payment pgPayment;
        PaidPayment paidPayment;

        try {
            pgPayment = portOneClient.getPayment().getPayment(request.getPaymentId()).get();

            if (!(pgPayment instanceof PaidPayment)) {
                throw new BusinessException(ErrorCode.PG_PAYMENT_NOT_PAID);
            }

            paidPayment = (PaidPayment) pgPayment;

            long pgAmount = paidPayment.getAmount().getTotal();
            if (pgAmount != payment.getAmount()) {
                log.warn("Payment amount mismatch detected. orderId={}, pgPaymentId={}, expectedAmount={}, pgAmount={}",
                        request.getOrderId(), request.getPaymentId(), payment.getAmount(), pgAmount);
                cancelMismatchedPayment(request, pgAmount);
                throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
            }

        } catch (BusinessException e) {
            if (e.getErrorCode() != ErrorCode.PAYMENT_AMOUNT_MISMATCH) {
                paymentAfterService.payExceptionProcess(request);
            }
            throw e;
        } catch (Exception e) {
            paymentAfterService.verifyRequiredProcess(request);
            throw new BusinessException(ErrorCode.PG_CALL_FAILED);
        }

        paymentAfterService.payAfterProcess(request, paidPayment);
        log.info("Payment confirm completed. orderId={}, pgPaymentId={}",
                request.getOrderId(), request.getPaymentId());
    }

    private void cancelMismatchedPayment(PaymentConfirmRequest request, long pgAmount) {
        try {
            portOneClient.getPayment().cancelPayment(
                    request.getPaymentId(),
                    pgAmount,
                    null,
                    null,
                    "Amount mismatch auto cancel",
                    null,
                    null,
                    null,
                    null
            ).get();
        } catch (Exception e) {
            log.warn("Payment amount mismatch cancel failed. orderId={}, pgPaymentId={}, pgAmount={}",
                    request.getOrderId(), request.getPaymentId(), pgAmount, e);
            paymentAfterService.cancelRequiredProcess(request);
            return;
        }

        log.info("Payment amount mismatch cancel succeeded. orderId={}, pgPaymentId={}, pgAmount={}",
                request.getOrderId(), request.getPaymentId(), pgAmount);
        paymentAfterService.illegalRequestProcess(request);
    }

    private String idempotencyKeyPrefix(String idempotencyKey) {
        if (idempotencyKey == null) {
            return null;
        }
        return idempotencyKey.length() <= 8 ? idempotencyKey : idempotencyKey.substring(0, 8);
    }
}
