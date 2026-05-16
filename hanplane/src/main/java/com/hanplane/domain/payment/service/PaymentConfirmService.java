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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentConfirmService {

    private final OrderRepository orderRepository;

    private final PaymentRepository paymentRepository;

    @Transactional
    public Payment confirmOrder(PaymentConfirmRequest request) {
        Order order = orderRepository.findById(request.getOrderId()).orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        OrderStatus orderStatus = order.getOrderStatus();

        if (orderStatus.equals(OrderStatus.CANCEL) || orderStatus.equals(OrderStatus.PAID) || orderStatus.equals(OrderStatus.EXPIRED)) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_IS_NOT_PENDING);
        }

        if (orderStatus.equals(OrderStatus.PENDING)) {
            Payment payment = Payment.builder()
                    .idempotencyKey(UUID.randomUUID().toString()).pgPaymentId(null)
                    .transactionId(null).payMethod(null)
                    .payStatus(PayStatus.PROCESSING).paidAt(null).amount(order.getTotalPrice()).order(order).build();
            order.updateOrderStatus(OrderStatus.PROCESSING);

            return paymentRepository.saveAndFlush(payment);
        } else {
            Payment payment = paymentRepository.findByOrderIdAndPayStatus(order.getId(), PayStatus.PROCESSING).orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

            return payment;
        }

    }

}
