package com.hanplane.domain.payment.repository;

import com.hanplane.domain.payment.entity.PayStatus;
import com.hanplane.domain.payment.entity.Payment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderIdAndPayStatus(Long orderId, PayStatus payStatus);

    Optional<Payment> findByOrder_IdAndIdempotencyKey(Long orderId, String idempotencyKey);

    List<Payment> findTop100ByPayStatusOrderByIdAsc(PayStatus payStatus);

    @Query("""
            select p from Payment p
            where p.payStatus = :payStatus
              and p.manualReviewRequired = false
              and p.compensationRetryCount < :maxRetryCount
              and (
                    p.lastCompensationTriedAt is null
                    or p.lastCompensationTriedAt <= :retryBefore
                  )
            order by p.id asc
            """)
    List<Payment> findCompensationTargets(
            @Param("payStatus") PayStatus payStatus,
            @Param("maxRetryCount") int maxRetryCount,
            @Param("retryBefore") LocalDateTime retryBefore,
            Pageable pageable
    );

    @Query("select p from Payment p join fetch p.order o join fetch o.orderItems where p.id = :paymentId")
    Optional<Payment> findByIdWithOrderAndItems(@Param("paymentId") Long paymentId);
}
