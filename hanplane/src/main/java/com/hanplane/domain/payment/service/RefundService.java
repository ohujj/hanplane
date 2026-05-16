package com.hanplane.domain.payment.service;

import com.hanplane.domain.order.entity.Order;
import com.hanplane.domain.order.entity.OrderItem;
import com.hanplane.domain.order.entity.OrderItemStatus;
import com.hanplane.domain.order.entity.OrderStatus;
import com.hanplane.domain.payment.dto.RefundRequest;
import com.hanplane.domain.payment.entity.Payment;
import com.hanplane.domain.payment.entity.Refund;
import com.hanplane.domain.payment.entity.RefundStatus;
import com.hanplane.domain.payment.repository.PaymentRepository;
import com.hanplane.domain.payment.repository.RefundRepository;
import com.hanplane.global.exception.BusinessException;
import com.hanplane.global.exception.ErrorCode;
import io.portone.sdk.server.PortOneClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundService {

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final PortOneClient portOneClient;

    public void RefundProcess(Long userId, RefundRequest refundRequest) {
        Payment payment = paymentRepository.findByIdWithOrderAndItems(refundRequest.getPaymentId()).orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        Order order = payment.getOrder();
        Long orderUserId = order.getUser().getId();
        if (!userId.equals(orderUserId)) {
            throw new BusinessException(ErrorCode.ILLEGAL_REQUEST_PARAMETER);
        }

        List<Long> requestIds = refundRequest.getOrderItemIds();

        List<OrderItem> orderItems = payment.getOrder().getOrderItems();

        List<Long> orderItemIds = orderItems.stream()
                .map(OrderItem::getId)
                .toList();

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

        //PG 환불 요청
        try {
            portOneClient.getPayment().cancelPayment(
                    payment.getPgPaymentId(),
                    (long) totalPrice,
                    null,
                    null,
                    "사용자 환불 요청",
                    null,
                    null,
                    null,
                    null
            ).get();
        } catch (Exception e) {
            refund.updateRefundStatus(RefundStatus.FAIL);
            refundRepository.save(refund);
            throw new BusinessException(ErrorCode.PG_CALL_FAILED);
        }
        //PG 환불 요청 끝

        refund.updateRefundStatus(RefundStatus.SUCCESS);
        refundRepository.save(refund);

        // requestIds에 해당하는 OrderItem REFUNDED로 변경
        orderItems.stream()
                .filter(item -> requestIds.contains(item.getId()))
                    .forEach(item -> item.updateStatus(OrderItemStatus.REFUNDED));
        // 전체 환불인지 판단
        boolean isFullRefund = orderItems.stream()
                .allMatch(item -> item.getStatus().equals(OrderItemStatus.REFUNDED));

        order.updateOrderStatus(isFullRefund ? OrderStatus.REFUNDED : OrderStatus.PARTIAL_REFUNDED);

    }
}
