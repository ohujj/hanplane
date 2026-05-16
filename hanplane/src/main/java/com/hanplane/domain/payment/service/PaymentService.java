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

    public void confirm(Long userId, PaymentConfirmRequest request) {
        Payment payment = paymentConfirmService.confirmOrder(userId, request);

        payProcess(request, payment);
    }

    public void payProcess(PaymentConfirmRequest request, Payment payment) {
        io.portone.sdk.server.payment.Payment pgPayment;
        PaidPayment paidPayment = null;

        try {
            pgPayment = portOneClient.getPayment().getPayment(request.getPaymentId()).get();

            if (!(pgPayment instanceof PaidPayment)) {
                throw new BusinessException(ErrorCode.PG_PAYMENT_NOT_PAID);
            }

            paidPayment = (PaidPayment) pgPayment;

            long pgAmount = paidPayment.getAmount().getTotal();
            if (pgAmount != payment.getAmount()) {
                paymentAfterService.illegalRequestProcess(request);
                throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
            }

        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.PAYMENT_AMOUNT_MISMATCH) {
                // illegalRequestProcess 이미 호출됨
            } else {
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
