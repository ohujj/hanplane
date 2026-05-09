package com.hanplane.domain.product.repository;

import com.hanplane.domain.product.dto.ProductSearchCondition;
import com.hanplane.domain.product.entity.Product;
import com.hanplane.domain.product.entity.QProduct;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepositoryCustom {

    private final JPAQueryFactory queryFactory;


    @Override
    public Page<Product> searchProduct(ProductSearchCondition productSearchCondition, Pageable pageable) {
        QProduct qProduct = QProduct.product;
        LocalDateTime expiredAt = productSearchCondition.getExpiredAt();
        String name = productSearchCondition.getName();
        Integer price = productSearchCondition.getPrice();

        JPAQuery<Long> count = queryFactory.select(qProduct.count())
                .from(qProduct)
                .where(name != null ? qProduct.name.contains(name) : null,
                        expiredAt != null ? qProduct.expiredAt.after(expiredAt) : null,
                        price != null ? qProduct.price.eq(price) : null);

        List<Product> list = queryFactory.selectFrom(qProduct)
                .where(name != null ? qProduct.name.contains(name) : null,
                        expiredAt != null ? qProduct.expiredAt.after(expiredAt) : null,
                        price != null ? qProduct.price.eq(price) : null)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();


        return PageableExecutionUtils.getPage(list, pageable, count::fetchOne);
    }
}
