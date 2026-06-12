package com.hanplane.domain.payment.repository;

import com.hanplane.domain.order.entity.Order;
import com.hanplane.domain.order.entity.OrderStatus;
import com.hanplane.domain.order.repository.OrderRepository;
import com.hanplane.domain.payment.entity.PayStatus;
import com.hanplane.domain.payment.entity.Payment;
import com.hanplane.domain.user.entity.Role;
import com.hanplane.domain.user.entity.User;
import com.hanplane.domain.user.repository.UserRepository;
import com.hanplane.global.config.QueryDslConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(QueryDslConfig.class)
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("same order and idempotencyKey cannot be saved twice")
    void duplicateOrderAndIdempotencyKeyThrows() {
        // given
        User user = userRepository.save(User.builder()
                .email("payment-test@test.com")
                .password("1234")
                .name("payment-test")
                .role(Role.USER)
                .build());

        Order order = orderRepository.save(Order.builder()
                .user(user)
                .totalPrice(10000)
                .orderStatus(OrderStatus.PENDING)
                .build());

        paymentRepository.saveAndFlush(createPayment(order, "same-key"));

        Payment duplicatePayment = createPayment(order, "same-key");

        // when & then
        assertThatThrownBy(() -> paymentRepository.saveAndFlush(duplicatePayment))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("compensation targets exclude recent, max retry, manual review, and other status payments")
    void findCompensationTargetsFiltersEligiblePayments() {
        // given
        User user = userRepository.save(User.builder()
                .email("compensation-test@test.com")
                .password("1234")
                .name("compensation-test")
                .role(Role.USER)
                .build());

        Order order = orderRepository.save(Order.builder()
                .user(user)
                .totalPrice(10000)
                .orderStatus(OrderStatus.PROCESSING)
                .build());

        Payment eligible = createPayment(order, "eligible", PayStatus.VERIFY_REQUIRED);
        Payment recent = createPayment(order, "recent", PayStatus.VERIFY_REQUIRED);
        Payment maxRetried = createPayment(order, "max-retried", PayStatus.VERIFY_REQUIRED);
        Payment manualReview = createPayment(order, "manual-review", PayStatus.VERIFY_REQUIRED);
        Payment otherStatus = createPayment(order, "other-status", PayStatus.CANCEL_REQUIRED);

        ReflectionTestUtils.setField(recent, "lastCompensationTriedAt", LocalDateTime.now());
        ReflectionTestUtils.setField(maxRetried, "compensationRetryCount", 3);
        ReflectionTestUtils.setField(manualReview, "manualReviewRequired", true);

        paymentRepository.saveAllAndFlush(List.of(eligible, recent, maxRetried, manualReview, otherStatus));

        // when
        List<Payment> result = paymentRepository.findCompensationTargets(
                PayStatus.VERIFY_REQUIRED,
                3,
                LocalDateTime.now().minusMinutes(5),
                PageRequest.of(0, 100)
        );

        // then
        assertThat(result).containsExactly(eligible);
    }

    @Test
    @DisplayName("manual review payments include only manual review targets and can be filtered by payStatus")
    void findManualReviewPaymentsFiltersManualReviewAndPayStatus() {
        // given
        User user = userRepository.save(User.builder()
                .email("manual-review-test@test.com")
                .password("1234")
                .name("manual-review-test")
                .role(Role.USER)
                .build());

        Order order = orderRepository.save(Order.builder()
                .user(user)
                .totalPrice(10000)
                .orderStatus(OrderStatus.ILLEGAL)
                .build());

        Payment cancelRequiredManual = createPayment(order, "cancel-required-manual", PayStatus.CANCEL_REQUIRED);
        Payment verifyRequiredManual = createPayment(order, "verify-required-manual", PayStatus.VERIFY_REQUIRED);
        Payment notManual = createPayment(order, "not-manual", PayStatus.CANCEL_REQUIRED);

        ReflectionTestUtils.setField(cancelRequiredManual, "manualReviewRequired", true);
        ReflectionTestUtils.setField(verifyRequiredManual, "manualReviewRequired", true);

        paymentRepository.saveAllAndFlush(List.of(cancelRequiredManual, verifyRequiredManual, notManual));

        // when
        List<Payment> allManualReviews = paymentRepository.findManualReviewPayments(null, PageRequest.of(0, 10)).getContent();
        List<Payment> cancelRequiredOnly = paymentRepository.findManualReviewPayments(PayStatus.CANCEL_REQUIRED, PageRequest.of(0, 10)).getContent();

        // then
        assertThat(allManualReviews).containsExactly(cancelRequiredManual, verifyRequiredManual);
        assertThat(cancelRequiredOnly).containsExactly(cancelRequiredManual);
    }

    private Payment createPayment(Order order, String idempotencyKey) {
        return createPayment(order, idempotencyKey, PayStatus.PROCESSING);
    }

    private Payment createPayment(Order order, String idempotencyKey, PayStatus payStatus) {
        return Payment.builder()
                .idempotencyKey(idempotencyKey)
                .pgPaymentId("pg-" + idempotencyKey)
                .payStatus(payStatus)
                .amount(order.getTotalPrice())
                .order(order)
                .build();
    }
}
