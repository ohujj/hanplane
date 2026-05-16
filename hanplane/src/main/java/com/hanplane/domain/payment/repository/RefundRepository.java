package com.hanplane.domain.payment.repository;

import com.hanplane.domain.payment.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRepository extends JpaRepository<Refund, Long> {
}
