package com.hanplane.domain.payment.service;

import com.hanplane.domain.order.entity.Order;
import com.hanplane.domain.order.entity.OrderStatus;
import com.hanplane.domain.order.repository.OrderRepository;
import com.hanplane.domain.payment.dto.PaymentConfirmRequest;
import com.hanplane.domain.payment.entity.PayStatus;
import com.hanplane.domain.payment.entity.Payment;
import com.hanplane.domain.payment.repository.PaymentRepository;
import com.hanplane.domain.user.entity.User;
import com.hanplane.global.exception.BusinessException;
import com.hanplane.global.exception.ErrorCode;
import com.hanplane.global.logging.TraceIdFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentConfirmServiceTest {

    @InjectMocks
    private PaymentConfirmService paymentConfirmService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRepository paymentRepository;

    private final Long userId = 1L;
    private final Long orderId = 10L;
    private final String idempotencyKey = "test-idem-key-uuid";

    private Order order;
    private User user;
    private PaymentConfirmRequest request;

    @BeforeEach
    void setUp() {
        MDC.put(TraceIdFilter.TRACE_ID, "trace-payment-confirm-test");

        user = mock(User.class);
        when(user.getId()).thenReturn(userId);

        order = mock(Order.class);
        when(order.getUser()).thenReturn(user);

        request = PaymentConfirmRequest.builder()
                .orderId(orderId)
                .paymentId("portone-payment-id-001")
                .build();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("same orderId and idempotencyKey returns existing Payment without locking or saving")
    void duplicateRequestReturnsExistingPaymentWithoutLocking() {
        // given
        Payment existingPayment = Payment.builder()
                .idempotencyKey(idempotencyKey)
                .payStatus(PayStatus.PROCESSING)
                .amount(10000)
                .order(order)
                .build();

        when(paymentRepository.findByOrder_IdAndIdempotencyKey(orderId, idempotencyKey))
                .thenReturn(Optional.of(existingPayment));

        // when
        PaymentConfirmResult result = paymentConfirmService.confirmOrder(userId, request, idempotencyKey);

        // then
        assertThat(result.newlyCreated()).isFalse();
        assertThat(result.payment()).isSameAs(existingPayment);

        verify(orderRepository, never()).findWithPessimisticLockById(any());
        verify(paymentRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("new Payment is created only after acquiring an Order pessimistic write lock")
    void newPaymentIsCreatedAfterOrderLock() {
        // given
        when(orderRepository.findWithPessimisticLockById(orderId)).thenReturn(Optional.of(order));
        when(order.getOrderStatus()).thenReturn(OrderStatus.PENDING);
        when(order.getTotalPrice()).thenReturn(10000);
        when(paymentRepository.findByOrder_IdAndIdempotencyKey(orderId, idempotencyKey))
                .thenReturn(Optional.empty(), Optional.empty());

        when(paymentRepository.saveAndFlush(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        PaymentConfirmResult result = paymentConfirmService.confirmOrder(userId, request, idempotencyKey);

        // then
        assertThat(result.newlyCreated()).isTrue();
        assertThat(result.payment().getIdempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(result.payment().getPayStatus()).isEqualTo(PayStatus.PROCESSING);
        assertThat(result.payment().getLastTraceId()).isEqualTo("trace-payment-confirm-test");

        InOrder inOrder = inOrder(orderRepository, paymentRepository);
        inOrder.verify(paymentRepository).findByOrder_IdAndIdempotencyKey(orderId, idempotencyKey);
        inOrder.verify(orderRepository).findWithPessimisticLockById(orderId);
        inOrder.verify(paymentRepository).findByOrder_IdAndIdempotencyKey(orderId, idempotencyKey);

        verify(orderRepository).findWithPessimisticLockById(orderId);
        verify(paymentRepository, times(2)).findByOrder_IdAndIdempotencyKey(orderId, idempotencyKey);
        verify(order).updateOrderStatus(OrderStatus.PROCESSING);
        verify(paymentRepository).saveAndFlush(any(Payment.class));
    }

    @Test
    @DisplayName("existing Payment found after lock is returned without creating another Payment")
    void existingPaymentAfterLockReturnsWithoutSaving() {
        // given
        Payment existingPayment = Payment.builder()
                .idempotencyKey(idempotencyKey)
                .payStatus(PayStatus.PROCESSING)
                .amount(10000)
                .order(order)
                .build();

        when(orderRepository.findWithPessimisticLockById(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrder_IdAndIdempotencyKey(orderId, idempotencyKey))
                .thenReturn(Optional.empty(), Optional.of(existingPayment));

        // when
        PaymentConfirmResult result = paymentConfirmService.confirmOrder(userId, request, idempotencyKey);

        // then
        assertThat(result.newlyCreated()).isFalse();
        assertThat(result.payment()).isSameAs(existingPayment);

        verify(orderRepository).findWithPessimisticLockById(orderId);
        verify(paymentRepository, times(2)).findByOrder_IdAndIdempotencyKey(orderId, idempotencyKey);
        verify(paymentRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("existing Payment owned by another user throws INVALID_REQUEST_PARAMETER")
    void existingPaymentOwnedByAnotherUserThrows() {
        // given
        Long anotherUserId = 999L;
        Payment existingPayment = Payment.builder()
                .idempotencyKey(idempotencyKey)
                .payStatus(PayStatus.PROCESSING)
                .amount(10000)
                .order(order)
                .build();

        when(user.getId()).thenReturn(anotherUserId);
        when(paymentRepository.findByOrder_IdAndIdempotencyKey(orderId, idempotencyKey))
                .thenReturn(Optional.of(existingPayment));

        // when & then
        assertThatThrownBy(() -> paymentConfirmService.confirmOrder(userId, request, idempotencyKey))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST_PARAMETER);

        verify(orderRepository, never()).findWithPessimisticLockById(any());
        verify(paymentRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("locked Order owned by another user throws INVALID_REQUEST_PARAMETER")
    void lockedOrderOwnedByAnotherUserThrows() {
        // given
        Long anotherUserId = 999L;
        when(user.getId()).thenReturn(anotherUserId);
        when(orderRepository.findWithPessimisticLockById(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrder_IdAndIdempotencyKey(orderId, idempotencyKey))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> paymentConfirmService.confirmOrder(userId, request, idempotencyKey))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST_PARAMETER);

        verify(orderRepository).findWithPessimisticLockById(orderId);
        verify(paymentRepository).findByOrder_IdAndIdempotencyKey(orderId, idempotencyKey);
        verify(paymentRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("non-PENDING Order without existing Payment throws ORDER_STATUS_IS_NOT_PENDING")
    void nonPendingOrderWithoutExistingPaymentThrows() {
        // given
        when(orderRepository.findWithPessimisticLockById(orderId)).thenReturn(Optional.of(order));
        when(order.getOrderStatus()).thenReturn(OrderStatus.PROCESSING);
        when(paymentRepository.findByOrder_IdAndIdempotencyKey(orderId, idempotencyKey))
                .thenReturn(Optional.empty(), Optional.empty());

        // when & then
        assertThatThrownBy(() -> paymentConfirmService.confirmOrder(userId, request, idempotencyKey))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_STATUS_IS_NOT_PENDING);

        verify(orderRepository).findWithPessimisticLockById(orderId);
        verify(paymentRepository, never()).saveAndFlush(any());
    }
}
