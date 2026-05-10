package com.hanplane.domain.order.repository;

import com.hanplane.domain.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long>
}
