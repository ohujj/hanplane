# hanplane

한정판 커머스 백엔드 프로젝트

hanplane은 한정판 상품 판매 상황에서 발생하는 재고 차감 동시성 문제와 결제 정합성을 학습하기 위한 Spring Boot 기반 커머스 백엔드 프로젝트입니다.

단순 CRUD 구현보다 다음 문제를 중심으로 설계했습니다.

- 한정 수량 상품 주문 시 재고 초과 판매를 어떻게 방지할 것인가
- 결제 승인 과정에서 PG 응답, 주문 금액, 내부 결제 상태를 어떻게 일관되게 맞출 것인가
- 쿠폰 발급처럼 경쟁 요청이 몰리는 기능에서 중복 발급과 수량 초과를 어떻게 방지할 것인가
- 검색 기능은 현재 서비스 규모에서 어느 수준의 복잡도가 적절한가

---

## 프로젝트 목적

hanplane은 한정판 상품 판매 도메인을 통해 백엔드 실무에서 자주 발생하는 정합성 문제를 실험하는 프로젝트입니다.

주요 학습 목표는 다음과 같습니다.

1. 재고 차감 동시성 문제 재현 및 해결 전략 비교
2. PortOne PG 연동을 통한 결제 승인 / 실패 / 환불 흐름 구현
3. JWT 기반 인증과 권한 분리
4. QueryDSL 기반 동적 검색 구현
5. Redis / Redisson을 활용한 분산락 적용
6. GitHub Actions와 EC2 기반 배포 자동화

---

## 기술 스택

| 구분 | 기술 |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3 |
| ORM | Spring Data JPA, QueryDSL |
| Database | MySQL 8, H2 |
| Cache / Lock | Redis, Redisson |
| Search | QueryDSL 기반 동적 검색 |
| Auth | Spring Security, JWT Access Token / Refresh Token |
| Payment | PortOne Server SDK |
| Test | JUnit 5, Spring Boot Test |
| Infra | AWS EC2, Docker, GitHub Actions |
| API Docs | Springdoc OpenAPI / Swagger UI |

---

## 핵심 기능

### 상품

- 상품 생성 / 수정 / 삭제
- 상품 목록 조회
- 상품명 / 가격 / 만료일 조건 기반 검색
- 주문 시 상품 재고 차감

### 주문

- 여러 상품을 포함한 주문 생성
- 주문 생성 시 재고 차감
- 주문 생성 시 쿠폰 할인 적용
- 주문 상태 관리

### 결제

- PortOne 결제 승인 정보 조회
- PG 결제 금액과 내부 주문 금액 검증
- 결제 성공 / 실패 상태 저장
- 결제 실패 시 보상 처리
- 환불 요청 및 환불 이력 저장

### 쿠폰

- 쿠폰 생성 / 수정 / 삭제
- 쿠폰 발급
- 유저별 보유 쿠폰 조회
- Redis 분산락 기반 쿠폰 발급 동시성 제어
- QueryDSL 기반 쿠폰 검색

### 인증

- 로그인
- Access Token 발급
- Refresh Token 저장 및 재발급
- Spring Security 기반 API 접근 제어

## 검색 설계

상품과 쿠폰 검색은 QueryDSL 기반 동적 검색으로 구현했습니다.

초기에는 Elasticsearch를 실험적으로 검토했지만, 현재 검색 조건은 상품명, 쿠폰명, 가격, 할인율, 만료일 중심이므로 RDB + QueryDSL로 충분하다고 판단했습니다.

따라서 운영 복잡도를 줄이기 위해 Elasticsearch 의존성을 제거하고 QueryDSL 검색으로 통일했습니다.

---

### 로컬 설정 파일

이 프로젝트는 로컬 실행 시 `application-local.yml`이 필요합니다.

실제 secret 값은 Git에 포함하지 않기 위해 `application-local.yml`은 `.gitignore` 처리합니다.

대신 예시 파일인 `application-local-example.yml`을 제공합니다.

```bash
cp src/main/resources/application-local-example.yml src/main/resources/application-local.yml
```

복사 후 아래 값을 본인 로컬 환경에 맞게 수정합니다.

DB username / password / JWT secret / PortOne secret / PortOne store id

## 도메인 구조

```text
com.hanplane
├── domain
│   ├── auth       # 로그인, 토큰 재발급, Refresh Token
│   ├── coupon     # 쿠폰, 유저 쿠폰, 쿠폰 발급 동시성
│   ├── order      # 주문, 주문 상품
│   ├── payment    # 결제 승인, 결제 후처리, 환불
│   ├── product    # 상품, 상품 검색, 재고 차감
│   └── user       # 사용자, 권한
└── global
    ├── config     # Security, QueryDSL, PortOne 설정
    ├── entity     # 공통 엔티티
    ├── exception  # 예외 처리
    ├── jwt        # JWT Provider, Filter, Principal
    └── response   # 공통 API 응답
```
