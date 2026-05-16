package com.hanplane.domain.payment.service;

import com.hanplane.domain.order.entity.Order;
import com.hanplane.domain.order.entity.OrderStatus;
import com.hanplane.domain.order.repository.OrderRepository;
import com.hanplane.domain.payment.repository.PaymentRepository;
import com.hanplane.domain.payment.dto.PaymentConfirmRequest;
import com.hanplane.domain.payment.entity.PayStatus;
import com.hanplane.domain.payment.entity.Payment;
import com.hanplane.global.exception.BusinessException;
import com.hanplane.global.exception.ErrorCode;
import io.portone.sdk.server.payment.PaidPayment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentAfterService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public void payAfterProcess(PaymentConfirmRequest request, PaidPayment paidPayment) {
        Payment payment = paymentRepository.findByOrderIdAndPayStatus(request.getOrderId(), PayStatus.PROCESSING).orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        Order order = orderRepository.findById(request.getOrderId()).orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        order.updateOrderStatus(OrderStatus.PAID);

        LocalDateTime paidAt = paidPayment.getPaidAt().atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime();
        String pgPaymentId = paidPayment.getId();
        String transactionId = paidPayment.getTransactionId();
        String payMethod = paidPayment.getMethod() != null ? paidPayment.getMethod().toString() : "UNKNOWN";

        payment.updateAfterPay(pgPaymentId, transactionId, payMethod, paidAt);
    }

    @Transactional
    public void payExceptionProcess(PaymentConfirmRequest request) {
        Payment payment = paymentRepository.findByOrderIdAndPayStatus(request.getOrderId(), PayStatus.PROCESSING).orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
        payment.updatePayStatus(PayStatus.FAIL);
    }

    @Transactional
    public void illegalRequestProcess(PaymentConfirmRequest request) {
        Payment payment = paymentRepository.findByOrderIdAndPayStatus(request.getOrderId(), PayStatus.PROCESSING).orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
        payment.updatePayStatus(PayStatus.ILLEGAL);

        Order order = orderRepository.findById(request.getOrderId()).orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        order.updateOrderStatus(OrderStatus.ILLEGAL);

        //pg 취소 호출
    }
}
