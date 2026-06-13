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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
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
    private static final String FAILURE_COMPENSATION_FAILED = "COMPENSATION_FAILED";
    private static final String FAILURE_PG_CANCEL_FAILED = "PG_CANCEL_FAILED";
    private static final String FAILURE_PG_CONNECTION_FAILED = "PG_CONNECTION_FAILED";
    private static final String FAILURE_PG_LOOKUP_FAILED = "PG_LOOKUP_FAILED";
    private static final String FAILURE_PG_TIMEOUT = "PG_TIMEOUT";

    private final PaymentRepository paymentRepository;
    private final PortOneClient portOneClient;

    @Value("${payment.compensation.batch-size:100}")
    private int batchSize;

    @Value("${payment.compensation.max-retry-count:5}")
    private int maxRetryCount;

    @Value("${payment.compensation.retry-interval-minutes:5}")
    private long retryIntervalMinutes;

    @Transactional
    public void retryCancelRequiredPayments() {
        List<Payment> payments = findTargets(PayStatus.CANCEL_REQUIRED);

        for (Payment payment : payments) {
            try {
                retryCancel(payment);
            } catch (Exception e) {
                recordFailure(payment, e, FAILURE_COMPENSATION_FAILED);
                log.warn("Unexpected CANCEL_REQUIRED payment compensation failure. paymentId={}, pgPaymentId={}",
                        payment.getId(), payment.getPgPaymentId(), e);
            }
        }
    }

    @Transactional
    public void retryVerifyRequiredPayments() {
        List<Payment> payments = findTargets(PayStatus.VERIFY_REQUIRED);

        for (Payment payment : payments) {
            try {
                retryVerify(payment);
            } catch (Exception e) {
                recordFailure(payment, e, FAILURE_COMPENSATION_FAILED);
                log.warn("Unexpected VERIFY_REQUIRED payment compensation failure. paymentId={}, pgPaymentId={}",
                        payment.getId(), payment.getPgPaymentId(), e);
            }
        }
    }

    private List<Payment> findTargets(PayStatus payStatus) {
        LocalDateTime retryBefore = LocalDateTime.now().minusMinutes(retryIntervalMinutes);
        return paymentRepository.findCompensationTargets(
                payStatus,
                maxRetryCount,
                retryBefore,
                PageRequest.of(0, batchSize)
        );
    }

    private void retryVerify(Payment payment) {
        io.portone.sdk.server.payment.Payment pgPayment;

        try {
            pgPayment = portOneClient.getPayment().getPayment(payment.getPgPaymentId()).get();
        } catch (Exception e) {
            recordFailure(payment, e, FAILURE_PG_LOOKUP_FAILED);
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
            recordFailure(payment, e, FAILURE_PG_CANCEL_FAILED);
            log.warn("CANCEL_REQUIRED payment cancel retry failed. paymentId={}, pgPaymentId={}",
                    payment.getId(), payment.getPgPaymentId(), e);
            return;
        }

        payment.updatePayStatus(PayStatus.ILLEGAL);
        payment.recordCompensationSuccess();
    }

    private void cancelMismatchedVerifiedPayment(Payment payment) {
        try {
            cancelFullPayment(payment.getPgPaymentId(), VERIFY_MISMATCH_CANCEL_REASON);
        } catch (Exception e) {
            payment.updateCancelRequired(payment.getPgPaymentId());
            markOrder(payment, OrderStatus.ILLEGAL);
            recordFailure(payment, e, FAILURE_PG_CANCEL_FAILED);
            return;
        }

        payment.updatePayStatus(PayStatus.ILLEGAL);
        markOrder(payment, OrderStatus.ILLEGAL);
        payment.recordCompensationSuccess();
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
        payment.recordCompensationSuccess();
    }

    private void markFailed(Payment payment) {
        payment.updatePayStatus(PayStatus.FAIL);
        markOrder(payment, OrderStatus.PENDING);
        payment.recordCompensationSuccess();
    }

    private void markOrder(Payment payment, OrderStatus orderStatus) {
        Order order = payment.getOrder();
        order.updateOrderStatus(orderStatus);
    }

    private void recordFailure(Payment payment, Exception e, String defaultFailureReason) {
        payment.recordCompensationFailure(toSafeFailureReason(e, defaultFailureReason), maxRetryCount);
    }

    private String toSafeFailureReason(Exception e, String defaultFailureReason) {
        String failureText = (e.getClass().getSimpleName() + " " + e.getMessage()).toLowerCase();
        if (failureText.contains("timeout") || failureText.contains("timed out")) {
            return FAILURE_PG_TIMEOUT;
        }
        if (failureText.contains("connect") || failureText.contains("connection")) {
            return FAILURE_PG_CONNECTION_FAILED;
        }
        return defaultFailureReason;
    }
}
