package com.hanplane.domain.payment.service;

import com.hanplane.domain.order.entity.Order;
import com.hanplane.domain.order.entity.OrderStatus;
import com.hanplane.domain.payment.entity.PayStatus;
import com.hanplane.domain.payment.entity.Payment;
import com.hanplane.domain.payment.repository.PaymentRepository;
import io.portone.sdk.server.PortOneClient;
import io.portone.sdk.server.payment.PaidPayment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentCompensationService {

    private static final String MISMATCH_CANCEL_RETRY_REASON = "Amount mismatch cancel retry";
    private static final String VERIFY_MISMATCH_CANCEL_REASON = "Verify required amount mismatch cancel";

    private final PaymentRepository paymentRepository;
    private final PortOneClient portOneClient;

    @Transactional
    public void retryCancelRequiredPayments() {
        List<Payment> payments = paymentRepository.findTop100ByPayStatusOrderByIdAsc(PayStatus.CANCEL_REQUIRED);

        for (Payment payment : payments) {
            retryCancel(payment);
        }
    }

    @Transactional
    public void retryVerifyRequiredPayments() {
        List<Payment> payments = paymentRepository.findTop100ByPayStatusOrderByIdAsc(PayStatus.VERIFY_REQUIRED);

        for (Payment payment : payments) {
            retryVerify(payment);
        }
    }

    private void retryVerify(Payment payment) {
        io.portone.sdk.server.payment.Payment pgPayment;

        try {
            pgPayment = portOneClient.getPayment().getPayment(payment.getPgPaymentId()).get();
        } catch (Exception e) {
            log.warn("VERIFY_REQUIRED payment lookup retry failed. paymentId={}, pgPaymentId={}",
                    payment.getId(), payment.getPgPaymentId(), e);
            return;
        }

        if (!(pgPayment instanceof PaidPayment paidPayment)) {
            markFailed(payment);
            return;
        }

        long pgAmount = paidPayment.getAmount().getTotal();
        if (pgAmount != payment.getAmount()) {
            cancelMismatchedVerifiedPayment(payment);
            return;
        }

        markPaid(payment, paidPayment);
    }

    private void retryCancel(Payment payment) {
        try {
            cancelFullPayment(payment.getPgPaymentId(), MISMATCH_CANCEL_RETRY_REASON);
        } catch (Exception e) {
            log.warn("CANCEL_REQUIRED payment cancel retry failed. paymentId={}, pgPaymentId={}",
                    payment.getId(), payment.getPgPaymentId(), e);
            return;
        }

        payment.updatePayStatus(PayStatus.ILLEGAL);
    }

    private void cancelMismatchedVerifiedPayment(Payment payment) {
        try {
            cancelFullPayment(payment.getPgPaymentId(), VERIFY_MISMATCH_CANCEL_REASON);
        } catch (Exception e) {
            payment.updateCancelRequired(payment.getPgPaymentId());
            markOrder(payment, OrderStatus.ILLEGAL);
            return;
        }

        payment.updatePayStatus(PayStatus.ILLEGAL);
        markOrder(payment, OrderStatus.ILLEGAL);
    }

    private void cancelFullPayment(String pgPaymentId, String reason) throws Exception {
        portOneClient.getPayment().cancelPayment(
                pgPaymentId,
                null,
                null,
                null,
                reason,
                null,
                null,
                null,
                null
        ).get();
    }

    private void markPaid(Payment payment, PaidPayment paidPayment) {
        LocalDateTime paidAt = paidPayment.getPaidAt().atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime();
        String transactionId = paidPayment.getTransactionId();
        String payMethod = paidPayment.getMethod() != null ? paidPayment.getMethod().toString() : "UNKNOWN";

        payment.updateAfterPay(transactionId, payMethod, paidAt);
        markOrder(payment, OrderStatus.PAID);
    }

    private void markFailed(Payment payment) {
        payment.updatePayStatus(PayStatus.FAIL);
        markOrder(payment, OrderStatus.PENDING);
    }

    private void markOrder(Payment payment, OrderStatus orderStatus) {
        Order order = payment.getOrder();
        order.updateOrderStatus(orderStatus);
    }
}
