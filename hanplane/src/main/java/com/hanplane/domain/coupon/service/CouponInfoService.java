package com.hanplane.domain.coupon.service;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import com.hanplane.domain.coupon.CouponCreateEvent;
import com.hanplane.domain.coupon.CouponDeleteEvent;
import com.hanplane.domain.coupon.CouponDocument;
import com.hanplane.domain.coupon.CouponUpdateEvent;
import com.hanplane.domain.coupon.dto.*;
import com.hanplane.domain.coupon.entity.Coupon;
import com.hanplane.domain.coupon.repository.CouponElasticsearchRepository;
import com.hanplane.domain.coupon.repository.CouponRepository;
import com.hanplane.domain.coupon.repository.UserCouponRepository;
import com.hanplane.global.exception.BusinessException;
import com.hanplane.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponInfoService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final ApplicationEventPublisher eventPublisher;
    @Autowired(required = false)
    private ElasticsearchOperations elasticsearchOperations;

    @Autowired(required = false)
    private CouponElasticsearchRepository couponElasticsearchRepository;


    public Page<CouponListResponse> getCouponList(Pageable pageable) {
        return couponRepository.findByDeletedAtIsNull(pageable)
                .map(CouponListResponse :: from);
    }

    public CouponListResponse getCouponDetail(Long couponId) {
        return couponRepository.findById(couponId).map(CouponListResponse::from).orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));
    }

    @Transactional
    public void updateCoupon(Long couponId, CouponUpdateRequest request) {
        Coupon coupon = couponRepository.findById(couponId).orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));
        if(request.getName() != null) {
            coupon.updateName(request.getName());
        }

        if(request.getExpiredAt() != null) {
            coupon.updateExpiredAt(request.getExpiredAt());
        }

        if(request.getDiscountRate() != null) {
            coupon.updateDiscountRate(request.getDiscountRate());
        }

        if(request.getTotalQuantity() != null) {
            coupon.updateTotalQuantity(request.getTotalQuantity());
        }

        eventPublisher.publishEvent(new CouponUpdateEvent(coupon));
    }

    @Transactional
    public void deleteCoupon(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId).orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));

        coupon.delete();

        eventPublisher.publishEvent(new CouponDeleteEvent(coupon.getId()));
    }

    public List<UserCouponResponse> getUserCouponByUserId(Long userId) {
        //return userCouponRepository.findByUserId(userId).stream()
        return userCouponRepository.findByUserIdWithCouponAndUser(userId).stream()
                .map(UserCouponResponse :: from)
                .collect(Collectors.toList());

    }

    public Page<CouponListResponse> searchCoupon(CouponSearchCondition condition, Pageable pageable) {

        return couponRepository.searchCoupons(condition, pageable).map(CouponListResponse::from);
    }

    public Page<CouponListResponse> elasticsearchCoupon(CouponSearchCondition condition, Pageable pageable) {

        if(couponElasticsearchRepository == null) {
            return searchCoupon(condition, pageable);
        }


        List<Query> musts = new ArrayList<>();

        String name = condition.getName();
        Integer discountRate = condition.getDiscountRate();
        LocalDateTime expiryDate = condition.getExpiryDate();

        if(name != null && !name.trim().isEmpty()) {
            musts.add(Query.of(q -> q.matchPhrase(m -> m.field("name").query(name))));
        }

        if(discountRate != null) {
            musts.add(Query.of(q -> q.match(m -> m.field("discountRate").query(discountRate))));
        }

        if(expiryDate != null) {
            String expiryDateStr = expiryDate.toString();
            musts.add(Query.of(q -> q.range(r -> r.untyped(u -> u.field("expiredAt").gte(JsonData.of(expiryDateStr))))));
        }

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q
                        .bool(b -> b.must(musts)))
                .withPageable(pageable)
                .build();

        SearchHits<CouponDocument> searchHits = elasticsearchOperations.search(query, CouponDocument.class);

        return new PageImpl<>(
                searchHits.getSearchHits().stream()
                        .map(hit -> CouponListResponse.from(hit.getContent()))
                        .collect(Collectors.toList()),
                pageable,
                searchHits.getTotalHits());
    }


    @Transactional
    public void createCoupon(CouponCreateRequest couponCreateRequest) {
        Coupon coupon = Coupon.builder()
                        .name(couponCreateRequest.getName())
                        .totalQuantity(couponCreateRequest.getTotalQuantity())
                        .discountRate(couponCreateRequest.getDiscountRate())
                        .expiredAt(couponCreateRequest.getExpiredAt())
                        .build();

        Coupon savedCoupon = couponRepository.save(coupon);

        eventPublisher.publishEvent(new CouponCreateEvent(savedCoupon));
    }
}
