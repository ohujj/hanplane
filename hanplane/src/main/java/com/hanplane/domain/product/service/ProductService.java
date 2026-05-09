package com.hanplane.domain.product.service;

import com.hanplane.domain.coupon.dto.CouponListResponse;
import com.hanplane.domain.product.dto.ProductCreateRequest;
import com.hanplane.domain.product.dto.ProductListResponse;
import com.hanplane.domain.product.dto.ProductSearchCondition;
import com.hanplane.domain.product.dto.ProductUpdateRequest;
import com.hanplane.domain.product.entity.Product;
import com.hanplane.domain.product.repository.ProductRepository;
import com.hanplane.global.exception.BusinessException;
import com.hanplane.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    public Page<ProductListResponse> searchProduct(ProductSearchCondition productSearchCondition, Pageable pageable) {
        return productRepository.searchProduct(productSearchCondition, pageable).map(ProductListResponse::from);
    }

    public ProductListResponse getProductDetail(Long productId) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        return ProductListResponse.from(product);
    }

    @Transactional
    public void updateProduct(Long productId, ProductUpdateRequest productUpdateRequest) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        if(productUpdateRequest.getAvailQuantity() != null) {
            product.updateAvailQuantity(productUpdateRequest.getAvailQuantity());
        }
        if(productUpdateRequest.getPrice() != null) {
            product.updatePrice(productUpdateRequest.getPrice());
        }
        if(productUpdateRequest.getName() != null) {
            product.updateName(productUpdateRequest.getName());
        }
        if(productUpdateRequest.getTotalQuantity() != null) {
            product.updateTotalQuantity(productUpdateRequest.getTotalQuantity());
        }
        if(productUpdateRequest.getExpiredAt() != null) {
            product.updateExpiredAt(productUpdateRequest.getExpiredAt());
        }

    }

    @Transactional
    public void deleteProduct(Long productId) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        product.deleteProduct();
    }

    @Transactional
    public void createProduct(ProductCreateRequest productCreateRequest) {
        Product product = Product.builder()
                .price(productCreateRequest.getPrice())
                .name(productCreateRequest.getName())
                .totalQuantity(productCreateRequest.getTotalQuantity())
                .availQuantity(productCreateRequest.getAvailQuantity())
                .expiredAt(productCreateRequest.getExpiredAt())
                .build();

        productRepository.save(product);
    }
}
