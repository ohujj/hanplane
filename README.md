# hanplane

한정판 커머스 백엔드 프로젝트

hanplane은 한정판 상품 판매 상황에서 발생할 수 있는 **주문, 결제, 환불, 재고 정합성 문제**를 학습하기 위한 Spring Boot 기반 커머스 백엔드 프로젝트입니다.

단순 CRUD 구현보다 다음 문제를 중심으로 설계했습니다.

* 한정 수량 상품 주문 시 재고 초과 판매를 어떻게 방지할 것인가
* 결제 승인 과정에서 PG 응답, 주문 금액, 내부 결제 상태를 어떻게 일관되게 맞출 것인가
* 환불 요청 시 결제 상태, 주문 소유자, 주문상품 상태를 어떻게 검증할 것인가
* 쿠폰 발급 시 중복 발급과 수량 초과를 어떻게 방지할 것인가
* 현재 서비스 규모에서 검색 기능은 어느 수준의 복잡도가 적절한가

---

## 프로젝트 목적

hanplane은 한정판 커머스 도메인을 통해 백엔드에서 자주 발생하는 **데이터 정합성 문제를 재현하고, 단순한 방식부터 검증해보는 학습 프로젝트**입니다.

주요 학습 목표는 다음과 같습니다.

1. 재고 차감 동시성 문제 재현 및 DB 비관적 락 적용
2. PortOne PG 연동을 통한 결제 승인 / 실패 / 환불 흐름 구현
3. 주문 / 결제 / 환불 상태 전이 관리
4. QueryDSL 기반 동적 검색 구현
5. GitHub Actions와 Docker 기반 배포 자동화 구성

---

## 핵심 문제와 해결 방식

| 문제                     | 해결 방식                                      | 검증                       |
| ---------------------- | ------------------------------------------ | ------------------------ |
| 동시 주문 시 재고 초과 판매 가능성   | 상품 재고 조회 구간에 DB Pessimistic Lock 적용        | MySQL 환경에서 동시 주문 테스트로 검증 |
| PG 결제 금액과 내부 주문 금액 불일치 | PG 승인 금액과 내부 주문 금액 비교 후 ILLEGAL 상태 분리      | 금액 불일치 결제 케이스 테스트        |
| 결제 승인 중 예외 발생          | Payment는 FAIL로 기록하고 Order는 PENDING으로 복구    | PG 승인 예외 케이스 테스트         |
| 중복 환불 요청               | OrderItem의 REFUNDED 상태 검증                  | 재환불 방지 테스트               |
| 타 사용자 환불 요청            | 주문 소유자 검증                                  | 타 사용자 주문 환불 방지 테스트       |
| 검색 복잡도 과잉              | Elasticsearch 제거 후 QueryDSL 기반 동적 검색으로 단순화 | 상품/쿠폰 조건 검색 테스트          |

---

## 기술 스택

| 구분        | 기술                              |
| --------- | ------------------------------- |
| Language  | Java 21                         |
| Framework | Spring Boot 3                   |
| ORM       | Spring Data JPA, QueryDSL       |
| Database  | MySQL 8, H2                     |
| Lock      | DB Pessimistic Lock             |
| Search    | QueryDSL 기반 동적 검색               |
| Auth      | Spring Security, JWT            |
| Payment   | PortOne Server SDK              |
| Test      | JUnit 5, Spring Boot Test       |
| Infra     | AWS EC2, Docker, GitHub Actions |
| API Docs  | Springdoc OpenAPI / Swagger UI  |

> EC2 배포는 GitHub Actions 기반 `deploy.yml`로 구성해 성공적으로 검증했습니다. 현재는 비용 관리를 위해 EC2 인스턴스를 중지한 상태입니다.

---

## 핵심 기능

### 상품

* 상품 생성 / 수정 / 삭제
* 상품 목록 조회
* 상품명 / 가격 / 만료일 조건 기반 검색
* 주문 시 상품 재고 차감
* 상품 재고 차감 시 DB 비관적 락 적용

### 주문

* 여러 상품을 포함한 주문 생성
* 주문 생성 시 상품 재고 차감
* 주문 생성 시 쿠폰 할인 적용
* 주문 상태 관리

### 결제

* PortOne 결제 승인 정보 조회
* PG 결제 금액과 내부 주문 금액 검증
* 결제 성공 / 실패 / 금액 불일치 상태 관리
* 결제 승인 중 예외 발생 시 Payment는 FAIL로 기록
* 결제 승인 실패 시 Order를 다시 결제 가능한 PENDING 상태로 복구

### 환불

* 환불 요청 및 환불 이력 저장
* SUCCESS 상태의 결제만 환불 가능하도록 검증
* 주문 소유자 검증
* 주문상품 환불 상태 검증
* 이미 환불된 주문상품의 재환불 방지
* 환불 금액 계산 검증

### 쿠폰

* 쿠폰 생성 / 수정 / 삭제
* 쿠폰 발급
* 유저별 보유 쿠폰 조회
* 유저별 중복 발급 및 제한 수량 초과 방지
* QueryDSL 기반 쿠폰 검색

### 인증

* 로그인
* Access Token 발급
* Refresh Token 저장 및 재발급
* Spring Security 기반 API 접근 제어

---

## 주요 설계 및 개선 내용

### 1. 재고 동시성 처리

한정 수량 상품은 동시에 여러 주문이 들어올 경우 재고 수량보다 많은 주문이 성공할 수 있습니다.

동시성 처리가 없는 상태에서 다수의 동시 주문 테스트를 수행해 Lost Update와 초과 판매 가능성을 확인했습니다. 이후 상품 재고 조회 구간에 DB 비관적 락을 적용해 동일 상품에 대한 재고 차감 요청이 순차 처리되도록 수정했습니다.

비관적 락 적용 후에는 MySQL 환경에서 재고 수량을 초과한 주문이 성공하지 않는지 검증했습니다.

H2 환경에서는 MySQL의 `SELECT FOR UPDATE` 동작을 완전히 보장하기 어려워, 비관적 락 동시성 검증은 MySQL 환경에서 별도로 확인했습니다.

### 2. 결제 정합성

결제 승인 과정에서는 PG 결제 금액과 내부 주문 금액이 일치해야 합니다.

PortOne PG 결제 승인 정보를 조회한 뒤, PG 결제 금액과 내부 주문 금액을 비교합니다. 금액이 일치하지 않는 경우 Payment와 Order를 ILLEGAL 상태로 분리해 정상 결제 흐름과 구분했습니다.

결제 승인 중 예외가 발생하면 Payment는 FAIL 상태로 기록하고, Order는 다시 결제 가능한 PENDING 상태로 복구해 PROCESSING 상태에 남지 않도록 처리했습니다.

현재 클라이언트 Idempotency-Key 기반 멱등성 처리는 후속 개선 과제로 분리했습니다.

### 3. 환불 정합성

환불 요청 시 다음 조건을 검증합니다.

* 결제가 SUCCESS 상태인지
* 주문이 환불 가능한 상태인지
* 요청 사용자가 주문 소유자인지
* 요청한 OrderItem이 해당 주문에 포함되어 있는지
* 이미 REFUNDED 상태인 OrderItem이 포함되어 있지 않은지

이를 통해 타 사용자 주문 환불, 미결제 주문 환불, 중복 환불 요청을 차단했습니다.

또한 환불 금액 계산, 재환불 방지, 타 사용자 주문 환불 방지 케이스를 테스트로 검증했습니다.

### 4. 검색 구조 단순화

상품과 쿠폰 검색은 QueryDSL 기반 동적 검색으로 구현했습니다.

초기에는 Elasticsearch를 실험적으로 검토했지만, 현재 검색 조건은 상품명, 쿠폰명, 가격, 할인율, 만료일 중심입니다. 별도의 검색 엔진을 운영할 만큼의 검색 요구사항이 아니라고 판단해 Elasticsearch 의존성을 제거하고 QueryDSL 검색으로 통일했습니다.

이를 통해 검색 기능은 유지하면서도 운영 복잡도와 관리 포인트를 줄였습니다.

---

## 주요 테스트 케이스

전체 테스트와 빌드는 아래 명령어로 실행합니다.

```bash
./gradlew clean build
```

Windows CMD 환경에서는 아래 명령어를 사용합니다.

```bat
gradlew.bat clean build
```

주요 테스트 케이스는 다음과 같습니다.

* 쿠폰 할인 적용 시 주문 총액 계산 검증
* PG 결제 승인 금액과 내부 주문 금액 불일치 검증
* PG 승인 예외 발생 시 Payment FAIL 기록 및 Order PENDING 복구 검증
* SUCCESS 상태가 아닌 결제 환불 차단 검증
* 타 사용자 주문 환불 차단 검증
* 이미 REFUNDED 상태인 OrderItem 재환불 차단 검증
* 주문 수량 기반 환불 금액 계산 검증
* MySQL 환경에서 재고 동시성 검증

일부 동시성 테스트는 MySQL의 `SELECT FOR UPDATE` 동작을 확인하기 위해 로컬 MySQL 환경에서 별도로 검증했습니다.

---

## 의도적으로 단순화한 부분

### Elasticsearch 제거

현재 검색 조건은 상품명, 쿠폰명, 가격, 할인율, 만료일 중심입니다. 복잡한 전문 검색이나 대용량 검색 요구사항이 없는 상태에서 Elasticsearch를 유지하면 운영 복잡도가 커진다고 판단했습니다.

따라서 Elasticsearch 의존성을 제거하고 QueryDSL 기반 동적 검색으로 단순화했습니다.

### Redis / Redisson 제거

재고 차감과 쿠폰 발급 동시성 처리는 DB 비관적 락 기반으로 정리했습니다.

현재 프로젝트는 단일 DB 기반 학습 프로젝트이며, 분산 환경에서 여러 애플리케이션 인스턴스가 동시에 락을 조율해야 하는 요구사항까지는 포함하지 않았습니다. 따라서 Redis/Redisson 기반 분산락은 운영 복잡도 대비 이점이 크지 않다고 판단해 제외했습니다.

### 결제 멱등성 미구현

결제 중복 요청 방지를 위한 클라이언트 Idempotency-Key 기반 멱등성 처리는 아직 구현하지 않았습니다.

현재는 결제 승인 금액 검증, 결제 실패 상태 처리, 주문 상태 복구, 환불 검증에 집중했고, Idempotency-Key 기반 중복 결제 방지는 후속 개선 과제로 분리했습니다.

---

## 로컬 실행 설정

로컬 실행 시 `application-local.yml`이 필요합니다.

실제 secret 값은 Git에 포함하지 않고, 예시 파일인 `application-local-example.yml`을 제공합니다.

```bash
cp src/main/resources/application-local-example.yml src/main/resources/application-local.yml
```

복사 후 아래 값을 본인 로컬 환경에 맞게 수정합니다.

* DB username
* DB password
* JWT secret
* PortOne secret
* PortOne store id

---

## 도메인 구조

```text
com.hanplane
├── domain
│   ├── auth       # 로그인, 토큰 재발급, Refresh Token
│   ├── coupon     # 쿠폰, 유저 쿠폰, 쿠폰 발급
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

---

## 한계와 후속 개선

* 클라이언트 Idempotency-Key 기반 결제 중복 요청 방지
* 결제 / 환불 요청 추적을 위한 요청 식별자 도입
* MySQL Testcontainers 기반 동시성 테스트 자동화
