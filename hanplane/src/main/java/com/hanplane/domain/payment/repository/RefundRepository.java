package com.hanplane.domain.payment.repository;

import com.hanplane.domain.payment.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    @Query("SELECT r FROM Refund r " +
            "JOIN FETCH r.payment p " +
            "JOIN FETCH p.order o " +
            "JOIN FETCH o.orderItems " +
            "WHERE r.id = :refundId")
    Optional<Refund> findByIdWithDetails(@Param("refundId") Long refundId);
}