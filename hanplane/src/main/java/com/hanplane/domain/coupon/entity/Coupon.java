package com.hanplane.domain.coupon.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupon")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int discountRate;

    @Column(nullable = false)
    private int totalQuantity;

    @Column(nullable = false)
    private int issuedQuantity;

    @Column(nullable = false)
    private LocalDateTime expiredAt;

    @Builder
    private Coupon(String name, int discountRate, int totalQuantity, LocalDateTime expiredAt) {
        this.name = name;
        this.discountRate = discountRate;
        this.totalQuantity = totalQuantity;
        this.issuedQuantity = 0;
        this.expiredAt = expiredAt;
    }
}
