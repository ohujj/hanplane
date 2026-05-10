package com.hanplane.domain.product.repository;

import com.hanplane.domain.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, ProductRepositoryCustom {

    Optional<Product> findByIdAndDeletedAtIsNull(Long id);
}
