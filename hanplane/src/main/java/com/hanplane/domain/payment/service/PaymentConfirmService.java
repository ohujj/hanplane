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

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentConfirmService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public PaymentConfirmResult confirmOrder(Long userId, PaymentConfirmRequest request, String idempotencyKey) {

        // 주문 조회
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        // orderId 변조 여부 검증
        if (!userId.equals(order.getUser().getId())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER);
        }

        // 멱등성 체크: 같은 orderId + idempotencyKey의 Payment가 이미 존재하면 재사용
        return paymentRepository.findByOrder_IdAndIdempotencyKey(order.getId(), idempotencyKey)
                .map(existing -> new PaymentConfirmResult(existing, false))
                .orElseGet(() -> createNewPayment(order, idempotencyKey));
    }

    // 기존 Payment가 없는 경우에만 호출
    private PaymentConfirmResult createNewPayment(Order order, String idempotencyKey) {
        // PENDING 이외의 상태는 모두 거부
        // - PROCESSING : 다른 idempotencyKey로 동일 주문에 재시도하는 경우 → 거부
        // - PAID/CANCEL/EXPIRED/ILLEGAL 등 : 이미 완료·취소된 주문 → 거부
        if (!order.getOrderStatus().equals(OrderStatus.PENDING)) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_IS_NOT_PENDING);
        }

        Payment payment = Payment.builder()
                .idempotencyKey(idempotencyKey)   // 클라이언트 헤더값 그대로 저장 (UUID.randomUUID() 제거)
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