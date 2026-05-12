package com.hanplane.domain.order.service;

import com.hanplane.domain.coupon.entity.UserCoupon;
import com.hanplane.domain.coupon.repository.CouponRepository;
import com.hanplane.domain.coupon.repository.UserCouponRepository;
import com.hanplane.domain.order.dto.OrderCreateRequest;
import com.hanplane.domain.order.dto.OrderItemRequest;
import com.hanplane.domain.order.repository.OrderItemRepository;
import com.hanplane.domain.order.repository.OrderRepository;
import com.hanplane.domain.product.repository.ProductRepository;
import com.hanplane.domain.user.entity.Role;
import com.hanplane.domain.user.entity.User;
import com.hanplane.domain.user.repository.UserRepository;
import com.hanplane.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserCouponRepository userCouponRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CouponRepository couponRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private OrderRepository orderRepository;
    @InjectMocks
    private OrderService orderService;

    @Test
    void 유저없음_예외발생() {
        Long userId = 1L;
        given(userRepository.findById(userId))
                .willReturn(Optional.empty());  // 빈 값 반환하도록 설정

        OrderCreateRequest request = OrderCreateRequest.builder()
                .orderItems(List.of())
                .build();

        // when & then
        assertThrows(BusinessException.class, () ->
                orderService.createOrder(request, userId));
    }

    @Test
    void 유저쿠폰없음_예외발생() {
        Long userId = 1L;
        User user = User.builder()
                .email("test").password("1234").role(Role.ADMIN).name("test").build();

        given(userRepository.findById(userId))
                .willReturn(Optional.of(user));  // 유저는 존재

        given(userCouponRepository.findByUserIdCouponFetch(userId))
                .willReturn(List.of());

        OrderCreateRequest request = OrderCreateRequest.builder()
                .orderItems(List.of()).couponId(1L).build();

        assertThrows(BusinessException.class, () ->
                orderService.createOrder(request, userId));

    }

}