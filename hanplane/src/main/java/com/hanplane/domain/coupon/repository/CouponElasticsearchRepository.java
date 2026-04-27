package com.hanplane.domain.coupon.repository;

import com.hanplane.domain.coupon.CouponDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface CouponElasticsearchRepository extends ElasticsearchRepository<CouponDocument, Long> {

    Page<CouponDocument> findByName(String keyword, Pageable pageable);

//    @Query("{\"wildcard\": {\"name\": {\"value\": \"*?0*\"}}}")
    @Query("{\"match\": {\"name\": \"?0\"}}")
    Page<CouponDocument> findByNameContaining(String keyword, Pageable pageable);



}