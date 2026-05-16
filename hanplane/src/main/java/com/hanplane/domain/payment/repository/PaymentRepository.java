package com.hanplane.domain.payment.repository;

import com.hanplane.domain.payment.entity.PayStatus;
import com.hanplane.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderIdAndPayStatus(Long orderId, PayStatus payStatus);

    @Query("select p from Payment p join fetch p.order o join fetch o.orderItems where p.id = :paymentId")
    Optional<Payment> findByIdWithOrderAndItems(@Param("paymentId") Long paymentId);
}
