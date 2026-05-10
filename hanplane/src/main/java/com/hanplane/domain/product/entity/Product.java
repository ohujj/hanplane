package com.hanplane.domain.product.entity;

import com.hanplane.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private int totalQuantity;

    @Column(nullable = false)
    private int availQuantity;

    @Column(nullable = false)
    private LocalDateTime expiredAt;

    @Builder
    private Product(String name, int price, int totalQuantity, int availQuantity, LocalDateTime expiredAt) {
        this.name = name;
        this.price = price;
        this.totalQuantity = totalQuantity;
        this.availQuantity = availQuantity;
        this.expiredAt = expiredAt;
    }

    public void updateName(String name) {
        this.name = name;
        update();
    }

    public void updateExpiredAt(LocalDateTime expiredAt) {
        this.expiredAt = expiredAt;
        update();
    }

    public void updateTotalQuantity(int totalQuantity) {
        this.totalQuantity = totalQuantity;
        update();
    }

    public void updateAvailQuantity(int availQuantity) {
        this.availQuantity = availQuantity;
        update();
    }

    public void updatePrice(int price) {
        this.price = price;
        update();
    }

    public void deleteProduct() {
        delete();
    }
}
