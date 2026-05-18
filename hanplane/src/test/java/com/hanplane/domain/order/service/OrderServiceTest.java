package com.hanplane.domain.order.service;

import com.hanplane.domain.coupon.entity.Coupon;
import com.hanplane.domain.coupon.entity.CouponStatus;
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
import com.hanplane.domain.user.entity.Role;
import com.hanplane.domain.user.entity.User;
import com.hanplane.domain.user.repository.UserRepository;
import com.hanplane.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

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

    @Test
    void 쿠폰없음_예외발생() {
        Long userId = 1L;

        User user = User.builder()
                .email("Test").password("1234").role(Role.ADMIN).name("test").build();

        given(userRepository.findById(userId))
                .willReturn(Optional.of(user));

        Coupon coupon = Coupon.builder().name("test").discountRate(50).totalQuantity(10).expiredAt(LocalDateTime.now().plusMonths(3)).build();

        UserCoupon userCoupon = UserCoupon.builder()
                .user(user).coupon(coupon).couponStatus(CouponStatus.UNUSED).build();

        given(userCouponRepository.findByUserIdCouponFetch(userId))
                .willReturn(List.of(userCoupon));

        given(couponRepository.findById(1L))
                .willReturn(Optional.empty());


        OrderCreateRequest request = OrderCreateRequest.builder()
                .orderItems(List.of()).couponId(1L).build();

        ReflectionTestUtils.setField(coupon, "id", 1L);

        assertThrows(BusinessException.class, () ->
                orderService.createOrder(request, userId)
        );


    }

    @Test
    void 상품없음_예외발생() {
        Long userId = 1L;

        User user = User.builder()
                .email("test").password("1234").role(Role.ADMIN).name("test").build();

        given(userRepository.findById(userId))
                .willReturn(Optional.of(user));

        OrderItemRequest orderItemRequest = OrderItemRequest.builder()
                .productId(1L).quantity(1).build();

        OrderCreateRequest request = OrderCreateRequest.builder()
                .orderItems(List.of(orderItemRequest)).couponId(null).build();

        given(productRepository.findByIdAndDeletedAtIsNull(1L))
                .willReturn(Optional.empty());

        assertThrows(BusinessException.class, () ->
                orderService.createOrder(request, userId)
        );

    }

    @Test
    void 쿠폰있음_주문성공() {
        User user = User.builder()
                .email("test").password("1234").role(Role.ADMIN).name("test").build();

        Long userId = 1L;

        given(userRepository.findById(userId))
                .willReturn(Optional.of(user));

        Coupon coupon = Coupon.builder()
                .name("test").discountRate(20).totalQuantity(10).expiredAt(LocalDateTime.now().plusMonths(6)).build();


        UserCoupon userCoupon = UserCoupon
                .builder()
                .user(user).coupon(coupon).couponStatus(CouponStatus.UNUSED).build();

        ReflectionTestUtils.setField(coupon, "id", 1L);

        Product product = Product.builder()
                .name("test").price(1000).totalQuantity(10).availQuantity(10).expiredAt(LocalDateTime.now().plusMonths(6)).build();

        ReflectionTestUtils.setField(userCoupon, "id", 1L);

        ReflectionTestUtils.setField(product, "id", 1L);

        given(userCouponRepository.findByUserIdCouponFetch(userId))
                .willReturn(List.of(userCoupon));


        Long couponId = 1L;

        given(couponRepository.findById(couponId))
                .willReturn(Optional.of(coupon));

        OrderItemRequest orderItemRequest = OrderItemRequest.builder()
                .productId(1L).quantity(3).build();

        List<OrderItemRequest> list = new ArrayList<>();
        list.add(orderItemRequest);

        OrderCreateRequest orderCreateRequest = OrderCreateRequest
                .builder()
                .orderItems(list).couponId(couponId).build();

        given(productRepository.findByIdAndDeletedAtIsNull(1L))
                .willReturn(Optional.of(product));

        orderService.createOrder(orderCreateRequest, userId);

        verify(orderItemRepository).saveAll(any());

    }

    @Test
    void 쿠폰없음_주문성공() {
        User user = User.builder()
                .email("test").password("1234").role(Role.ADMIN).name("test").build();

        Long userId = 1L;

        given(userRepository.findById(userId))
                .willReturn(Optional.of(user));

        Product product = Product.builder()
                .name("test").price(1000).totalQuantity(10).availQuantity(10).expiredAt(LocalDateTime.now().plusMonths(6)).build();

        ReflectionTestUtils.setField(product, "id", 1L);

        OrderItemRequest orderItemRequest = OrderItemRequest.builder()
                .productId(1L).quantity(3).build();

        List<OrderItemRequest> list = new ArrayList<>();
        list.add(orderItemRequest);

        OrderCreateRequest orderCreateRequest = OrderCreateRequest
                .builder()
                .orderItems(list).couponId(null).build();

        given(productRepository.findByIdAndDeletedAtIsNull(1L))
                .willReturn(Optional.of(product));

        orderService.createOrder(orderCreateRequest, userId);

        verify(orderItemRepository).saveAll(any());

    }

    @Test
    void 주문시_OrderItem_price는_단가만_저장된다() {
        Long userId = 1L;
        User user = User.builder().email("test").password("1234").role(Role.ADMIN).name("test").build();
        Product product = Product.builder()
                .name("한정판").price(10000).totalQuantity(10).availQuantity(10)
                .expiredAt(LocalDateTime.now().plusMonths(6)).build();
        ReflectionTestUtils.setField(product, "id", 1L);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userCouponRepository.findByUserIdCouponFetch(userId)).willReturn(List.of());
        given(productRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(product));

        OrderCreateRequest request = OrderCreateRequest.builder()
                .orderItems(List.of(OrderItemRequest.builder().productId(1L).quantity(3).build()))
                .build();

        orderService.createOrder(request, userId);

        ArgumentCaptor<List<OrderItem>> captor = ArgumentCaptor.forClass(List.class);
        verify(orderItemRepository).saveAll(captor.capture());

        OrderItem savedItem = captor.getValue().get(0);
        assertThat(savedItem.getPrice()).isEqualTo(10000);  // 단가 (수량 곱하지 않음)
        assertThat(savedItem.getQuantity()).isEqualTo(3);
    }

    @Test
    void 쿠폰_20퍼센트_할인_주문_총액은_24000() {
        Long userId = 1L;
        User user = User.builder().email("test").password("1234").role(Role.ADMIN).name("test").build();
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        Coupon coupon = Coupon.builder()
                .name("20%쿠폰").discountRate(20).totalQuantity(10)
                .expiredAt(LocalDateTime.now().plusMonths(6)).build();
        ReflectionTestUtils.setField(coupon, "id", 1L);

        UserCoupon userCoupon = UserCoupon.builder()
                .user(user).coupon(coupon).couponStatus(CouponStatus.UNUSED).build();
        ReflectionTestUtils.setField(userCoupon, "id", 1L);

        given(userCouponRepository.findByUserIdCouponFetch(userId)).willReturn(List.of(userCoupon));
        given(couponRepository.findById(1L)).willReturn(Optional.of(coupon));

        Product product = Product.builder()
                .name("한정판").price(10000).totalQuantity(10).availQuantity(10)
                .expiredAt(LocalDateTime.now().plusMonths(6)).build();
        ReflectionTestUtils.setField(product, "id", 1L);
        given(productRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(product));

        OrderCreateRequest request = OrderCreateRequest.builder()
                .orderItems(List.of(OrderItemRequest.builder().productId(1L).quantity(3).build()))
                .couponId(1L)
                .build();

        orderService.createOrder(request, userId);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());

        assertThat(orderCaptor.getValue().getTotalPrice()).isEqualTo(24000);
    }
}