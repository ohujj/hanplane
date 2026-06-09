package com.hanplane.domain.payment.service;

import com.hanplane.domain.order.entity.Order;
import com.hanplane.domain.order.entity.OrderStatus;
import com.hanplane.domain.order.repository.OrderRepository;
import com.hanplane.domain.payment.dto.PaymentConfirmRequest;
import com.hanplane.domain.payment.entity.PayStatus;
import com.hanplane.domain.payment.entity.Payment;
import com.hanplane.domain.payment.repository.PaymentRepository;
import com.hanplane.global.exception.BusinessException;
import com.hanplane.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentConfirmService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public PaymentConfirmResult confirmOrder(Long userId, PaymentConfirmRequest request, String idempotencyKey) {
        Long orderId = request.getOrderId();

        return paymentRepository.findByOrder_IdAndIdempotencyKey(orderId, idempotencyKey)
                .map(existing -> {
                    validateOrderOwner(userId, existing.getOrder());
                    return new PaymentConfirmResult(existing, false);
                })
                .orElseGet(() -> confirmWithOrderLock(userId, orderId, idempotencyKey));
    }

    private PaymentConfirmResult confirmWithOrderLock(Long userId, Long orderId, String idempotencyKey) {
        Order order = orderRepository.findWithPessimisticLockById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        validateOrderOwner(userId, order);

        return paymentRepository.findByOrder_IdAndIdempotencyKey(orderId, idempotencyKey)
                .map(existing -> new PaymentConfirmResult(existing, false))
                .orElseGet(() -> createNewPayment(order, idempotencyKey));
    }

    private void validateOrderOwner(Long userId, Order order) {
        if (!userId.equals(order.getUser().getId())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER);
        }
    }

    private PaymentConfirmResult createNewPayment(Order order, String idempotencyKey) {
        if (!order.getOrderStatus().equals(OrderStatus.PENDING)) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_IS_NOT_PENDING);
        }

        Payment payment = Payment.builder()
                .idempotencyKey(idempotencyKey)
                .pgPaymentId(null)
                .transactionId(null)
                .payMethod(null)
                .payStatus(PayStatus.PROCESSING)
                .paidAt(null)
                .amount(order.getTotalPrice())
                .order(order)
                .build();

        order.updateOrderStatus(OrderStatus.PROCESSING);

        return new PaymentConfirmResult(paymentRepository.saveAndFlush(payment), true);
    }
}
