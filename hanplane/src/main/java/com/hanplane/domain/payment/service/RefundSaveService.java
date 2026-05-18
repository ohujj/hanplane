package com.hanplane.domain.payment.service;

import com.hanplane.domain.order.entity.Order;
import com.hanplane.domain.order.entity.OrderItem;
import com.hanplane.domain.order.entity.OrderItemStatus;
import com.hanplane.domain.order.entity.OrderStatus;
import com.hanplane.domain.payment.dto.RefundRequest;
import com.hanplane.domain.payment.entity.PayStatus;
import com.hanplane.domain.payment.entity.Payment;
import com.hanplane.domain.payment.entity.Refund;
import com.hanplane.domain.payment.entity.RefundStatus;
import com.hanplane.domain.payment.repository.PaymentRepository;
import com.hanplane.domain.payment.repository.RefundRepository;
import com.hanplane.global.exception.BusinessException;
import com.hanplane.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RefundSaveService {

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public Refund refundSaveProcess(Long userId, RefundRequest refundRequest) {
        Payment payment = paymentRepository.findByIdWithOrderAndItems(refundRequest.getPaymentId()).orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        PayStatus payStatus = payment.getPayStatus();
        if (!payStatus.equals(PayStatus.SUCCESS)) {
            throw new BusinessException(ErrorCode.PAYMENT_IS_NOT_SUCCESS);
        }

        Order order = payment.getOrder();

        OrderStatus orderStatus = order.getOrderStatus();

        if (!orderStatus.equals(OrderStatus.PAID) && !orderStatus.equals(OrderStatus.PARTIAL_REFUNDED)) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_IS_NOT_PAID);
        }

        Long orderUserId = order.getUser().getId();
        if (!userId.equals(orderUserId)) {
            throw new BusinessException(ErrorCode.ILLEGAL_REQUEST_PARAMETER);
        }

        List<Long> requestIds = refundRequest.getOrderItemIds();

        List<OrderItem> orderItems = payment.getOrder().getOrderItems();

        List<Long> orderItemIds = orderItems.stream()
                .map(OrderItem::getId)
                .toList();

        boolean hasRefunded = orderItems.stream()
                .filter(item -> requestIds.contains(item.getId()))
                .anyMatch(item -> item.getStatus().equals(OrderItemStatus.REFUNDED));

        if (hasRefunded) {
            throw new BusinessException(ErrorCode.ALREADY_REFUNDED_ORDER_ITEM);
        }

        if (!orderItemIds.containsAll(requestIds)) {
            throw new BusinessException(ErrorCode.ILLEGAL_REQUEST_PARAMETER);
        }

        int totalPrice = orderItems.stream()
                .filter(item -> requestIds.contains(item.getId()))
                .mapToInt(item -> item.getPrice() * item.getQuantity())
                .sum();

        if (totalPrice == 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER);
        }

        List<OrderItem> refundItems = orderItems.stream()
                .filter(item -> requestIds.contains(item.getId()))
                .toList();

        Refund refund = Refund.builder()
                .payment(payment).orderItems(refundItems).status(RefundStatus.PENDING).amount(totalPrice).build();

        return refundRepository.save(refund);
    }

    // 변경 후
    @Transactional
    public void refundAfterProcess(Long refundId, RefundRequest refundRequest) {
        // 새 트랜잭션에서 managed entity로 재조회
        // 이유: refundSaveProcess() 트랜잭션 종료 후 Refund는 detached 상태 →
        //       파라미터로 받으면 dirty checking 안 됨 → DB 미반영 버그
        Refund refund = refundRepository.findByIdWithDetails(refundId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFUND_NOT_FOUND));

        refund.updateRefundStatus(RefundStatus.SUCCESS);

        Payment payment = refund.getPayment();
        List<Long> requestIds = refundRequest.getOrderItemIds();
        List<OrderItem> orderItems = payment.getOrder().getOrderItems();

        orderItems.stream()
                .filter(item -> requestIds.contains(item.getId()))
                .forEach(item -> item.updateStatus(OrderItemStatus.REFUNDED));

        boolean isFullRefund = orderItems.stream()
                .allMatch(item -> item.getStatus().equals(OrderItemStatus.REFUNDED));

        Order order = payment.getOrder();
        order.updateOrderStatus(isFullRefund ? OrderStatus.REFUNDED : OrderStatus.PARTIAL_REFUNDED);
    }

}
