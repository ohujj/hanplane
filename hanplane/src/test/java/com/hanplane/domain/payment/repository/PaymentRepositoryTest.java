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
import org.springframework.dao.DataIntegrityViolationException;

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

    private Payment createPayment(Order order, String idempotencyKey) {
        return Payment.builder()
                .idempotencyKey(idempotencyKey)
                .payStatus(PayStatus.PROCESSING)
                .amount(order.getTotalPrice())
                .order(order)
                .build();
    }
}
