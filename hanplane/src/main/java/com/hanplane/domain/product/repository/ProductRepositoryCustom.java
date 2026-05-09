package com.hanplane.domain.product.repository;

import com.hanplane.domain.product.dto.ProductSearchCondition;
import com.hanplane.domain.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductRepositoryCustom {

    Page<Product> searchProduct(ProductSearchCondition productSearchCondition, Pageable pageable);
}
