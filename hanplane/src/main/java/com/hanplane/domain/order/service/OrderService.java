package com.hanplane.domain.order.service;

import com.hanplane.domain.coupon.entity.Coupon;
import com.hanplane.domain.coupon.entity.UserCoupon;
import com.hanplane.domain.coupon.repository.CouponRepository;
import com.hanplane.domain.coupon.repository.UserCouponRepository;
import com.hanplane.domain.order.dto.OrderCreateRequest;
import com.hanplane.domain.order.dto.OrderItemRequest;
import com.hanplane.domain.order.entity.Order;
import com.hanplane.domain.order.entity.OrderItem;
import com.hanplane.domain.order.entity.OrderStatus;
import com.hanplane.domain.order.repository.OrderItemRepository;
import com.hanplane.domain.order.repository.OrderRepository;
import com.hanplane.domain.product.entity.Product;
import com.hanplane.domain.product.repository.ProductRepository;
import com.hanplane.domain.user.entity.User;
import com.hanplane.domain.user.repository.UserRepository;
import com.hanplane.global.exception.BusinessException;
import com.hanplane.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final UserRepository userRepository;
    private final UserCouponRepository userCouponRepository;
    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public void createOrder(OrderCreateRequest orderCreateRequest, Long userId) {

        //user가 실제로 존재하는지 및 유저 찾기
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        //flow1 : 해당 유저가 해당 쿠폰을 보유하고 있는지
        List<UserCoupon> userCouponsByUserId = userCouponRepository.findByUserIdCouponFetch(userId);
        List<Long> userCouponIdList = userCouponsByUserId.stream()
                .map(uc -> uc.getCoupon().getId()).toList();

        Long couponId = orderCreateRequest.getCouponId();

        int discountRate = 0;
        Coupon coupon = null;
        if(couponId != null) {
            if(!userCouponIdList.contains(couponId)) {
                throw new BusinessException(ErrorCode.USER_NOT_HAVE_COUPON_ID);
            }

            //쿠폰이 실제로 존재하는지 및 쿠폰 찾기
            coupon = couponRepository.findById(orderCreateRequest.getCouponId()).orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));
            discountRate = coupon.getDiscountRate();
        }


        List<OrderItemRequest> orderItems = orderCreateRequest.getOrderItems();
        Map<Long, Product> productMap = new HashMap<>();
        int totalPrice = 0;

        for(OrderItemRequest request : orderItems) {

            Product product = productRepository.findByIdAndDeletedAtIsNull(request.getProductId()).orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
            productMap.put(product.getId(), product);

            product.decrease(request.getQuantity());

            int productPrice = product.getPrice() * request.getQuantity();
            totalPrice += productPrice;
        }

        if(discountRate != 0) {
            totalPrice -= (totalPrice / discountRate);
        }

        Order order = Order.builder()
                .user(user)
                .coupon(coupon)
                .totalPrice(totalPrice)
                .orderStatus(OrderStatus.PENDING)
                .build();

        orderRepository.save(order);

        List<OrderItem> saveOrderItemList = new ArrayList<>();
        for (OrderItemRequest items : orderItems) {

            //map에서 product 가져옴
            Product product = productMap.get(items.getProductId());

            //flow3 : 상품별 가격으로 order_item에 저장하기(저장 처리는 Order 생성 끝난 후)
            int productPrice = product.getPrice() * items.getQuantity();
            OrderItem orderItem = OrderItem.builder()
                    .price(productPrice)
                    .quantity(items.getQuantity())
                    .product(product)
                    .order(order)
                    .build();

            saveOrderItemList.add(orderItem);
        }

        orderItemRepository.saveAll(saveOrderItemList);

    }

}
