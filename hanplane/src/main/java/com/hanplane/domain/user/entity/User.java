package com.hanplane.domain.user.entity;

import com.hanplane.domain.coupon.entity.UserCoupon;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity //JPA가 관리하는 객체임을 선언
@Table(name = "users") //users 라는 테이블로 매핑
@Getter //롬복의 게터
@NoArgsConstructor(access = AccessLevel.PROTECTED) //기본 생성자 생성, but protected 레벨로 생성해서 무차별 생성 방지(JPA를 위해 필요한 어노테이션임)
public class User {

    @Id //PK 선언
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true) // not null + unique 키 선언
    private String email;

    @Column(nullable = false)
    private String password;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY) // 1 대 다 관계, user컬럼과 매핑, lazy로 해서 n+1 문제 제어
    private List<UserCoupon> userCoupons = new ArrayList<>();

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private Role role;

    @Builder // 생성자, 클래스 레벨에 붙이면 @Id인 PK까지 건들게 되어 문제가 생겨서 여기에 건다.
    private User(String email, String password, Role role) {
        this.email = email;
        this.password = password;
        this.role = role;
    }
}
