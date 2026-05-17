package com.hanplane;

import com.hanplane.domain.order.dto.OrderCreateRequest;
import com.hanplane.domain.order.dto.OrderItemRequest;
import com.hanplane.domain.order.repository.OrderItemRepository;
import com.hanplane.domain.order.repository.OrderRepository;
import com.hanplane.domain.order.service.OrderService;
import com.hanplane.domain.product.entity.Product;
import com.hanplane.domain.product.repository.ProductRepository;
import com.hanplane.domain.user.entity.Role;
import com.hanplane.domain.user.entity.User;
import com.hanplane.domain.user.repository.UserRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest
public class ProductConcurrencyTest {

    @Autowired
    private OrderService orderService;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;

    @AfterEach
    void cleanUp() {
        // 외래키 의존성 역순으로 삭제
        orderItemRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    void 동시에_100명이_재고_10개_상품_주문() throws InterruptedException {
        // ===== given =====
        // 재고 10개짜리 한정판 상품
        Product product = Product.builder()
                .name("한정판 상품")
                .price(10000)
                .totalQuantity(10)
                .availQuantity(10)
                .expiredAt(LocalDateTime.now().plusDays(30))
                .build();
        Product savedProduct = productRepository.save(product);

        // 100명 유저 생성
        List<User> userList = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            User user = User.builder()
                    .email("user" + i + "@test.com")
                    .password("testUser" + i)
                    .name("user" + i)      // ← 추가
                    .role(Role.USER)        // ← 추가
                    .build();
            userList.add(userRepository.save(user));
        }

        // ===== when =====
        long start = System.currentTimeMillis();

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(100);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (User user : userList) {
            Long userId = user.getId();

            new Thread(() -> {
                try {
                    startLatch.await();

                    // 주문 요청 생성 (1개씩 주문)
                    OrderCreateRequest request = OrderCreateRequest.builder()
                            .orderItems(List.of(
                                    OrderItemRequest.builder()
                                            .productId(savedProduct.getId())
                                            .quantity(1)
                                            .build()
                            ))
                            .build();

                    orderService.createOrder(request, userId);
                    successCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    if (failCount.get() <= 3) {
                        e.printStackTrace();   // ← 이 줄 추가
                    }
                } finally {
                    endLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        endLatch.await();

        long end = System.currentTimeMillis();

        // ===== then =====
        Product result = productRepository.findById(savedProduct.getId()).orElseThrow();

        System.out.println("======================================");
        System.out.println("소요 시간: " + (end - start) + "ms");
        System.out.println("성공 카운트: " + successCount.get());
        System.out.println("실패 카운트: " + failCount.get());
        System.out.println("최종 재고: " + result.getAvailQuantity());
        System.out.println("======================================");

        // 동시성 처리 없으면 이 두 단언 중 하나 이상은 실패함
        Assertions.assertThat(result.getAvailQuantity()).isEqualTo(0);
        Assertions.assertThat(successCount.get()).isEqualTo(10);
    }
}