package com.hanplane.domain.product.controller;

import com.hanplane.domain.coupon.dto.CouponListResponse;
import com.hanplane.domain.product.dto.ProductCreateRequest;
import com.hanplane.domain.product.dto.ProductListResponse;
import com.hanplane.domain.product.dto.ProductSearchCondition;
import com.hanplane.domain.product.dto.ProductUpdateRequest;
import com.hanplane.domain.product.service.ProductService;
import com.hanplane.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<ProductListResponse>>> searchProduct(@ModelAttribute ProductSearchCondition productSearchCondition, Pageable pageable) {
        Page<ProductListResponse> productList = productService.searchProduct(productSearchCondition, pageable);

        return ResponseEntity.ok(ApiResponse.success(productList));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductListResponse>> getProductDetail(@PathVariable("productId") Long productId) {
        ProductListResponse productDetail = productService.getProductDetail(productId);

        return ResponseEntity.ok(ApiResponse.success(productDetail));
    }

    @PatchMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> updateProduct(@PathVariable("productId") Long productId, @RequestBody @Valid ProductUpdateRequest productUpdateRequest) {
        productService.updateProduct(productId, productUpdateRequest);

        return ResponseEntity.ok(ApiResponse.success());
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable("productId") Long productId) {
        productService.deleteProduct(productId);

        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping()
    public ResponseEntity<ApiResponse<Void>> createProduct(@RequestBody @Valid ProductCreateRequest productCreateRequest) {
        productService.createProduct(productCreateRequest);

        return ResponseEntity.ok(ApiResponse.success());
    }


}