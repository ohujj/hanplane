# hanplane

한정판 커머스 도메인의 **주문/결제/환불/재고 정합성 문제**를 다루는 Spring Boot 기반 백엔드 프로젝트입니다.

단순 CRUD 구현보다 다음과 같은 백엔드 핵심 문제를 재현하고 해결하는 데 집중했습니다.

* 한정 수량 상품 주문 시 재고 초과 판매를 어떻게 방지할 것인가
* 결제 confirm API가 중복 호출될 때 Payment 중복 생성을 어떻게 막을 것인가
* PG 조회 실패/타임아웃처럼 결제 결과를 확정할 수 없는 상태를 어떻게 보정할 것인가
* PG 승인 금액과 내부 주문 금액이 불일치할 때 정상 주문과 어떻게 분리할 것인가
* 환불 요청 시 결제 상태, 주문 소유자, 주문상품 상태를 어떻게 검증할 것인가
* 현재 서비스 규모에서 검색 기능은 어느 수준의 복잡도가 적절한가

---

## 프로젝트 목적

hanplane은 한정판 커머스 도메인을 통해 백엔드에서 자주 발생하는 **데이터 정합성, 동시성, 외부 PG 연동 예외 상황**을 직접 구현하고 검증하기 위한 프로젝트입니다.

주요 학습 및 구현 목표는 다음과 같습니다.

1. 재고 차감 동시성 문제 재현 및 DB 비관적 락 적용
2. PortOne PG 연동 기반 결제 승인/실패/환불 흐름 구현
3. 결제 confirm 중복 요청에 대한 멱등 처리
4. PG 조회 실패/타임아웃 상태 보정
5. 금액 불일치 결제의 취소 시도 및 보정 처리
6. 주문/결제/환불 상태 전이 관리
7. QueryDSL 기반 동적 검색 구현
8. GitHub Actions와 Docker 기반 배포 자동화 구성

---

## 기술 스택

| 구분        | 기술                              |
| --------- | ------------------------------- |
| Language  | Java 21                         |
| Framework | Spring Boot 3                   |
| ORM       | Spring Data JPA, QueryDSL       |
| Database  | MySQL 8, H2                     |
| Lock      | DB Pessimistic Lock             |
| Auth      | Spring Security, JWT            |
| Payment   | PortOne Server SDK              |
| Test      | JUnit 5, Spring Boot Test       |
| Infra     | AWS EC2, Docker, GitHub Actions |
| API Docs  | Springdoc OpenAPI / Swagger UI  |

> EC2 배포는 GitHub Actions 기반 `deploy.yml`로 구성해 검증했습니다.
> 현재는 비용 관리를 위해 EC2 인스턴스를 중지한 상태입니다.

---

## ERD

ERD는 dbdiagram.io를 사용해 작성했습니다.

- ERD: https://dbdiagram.io/d/6a2976615c789b8acb552076

결제 confirm 멱등 처리를 위해 `payment(order_id, idempotency_key)` unique constraint를 적용했습니다.  
동일 주문에 대한 Payment 생성 흐름은 Order row lock으로 직렬화하고, 같은 주문/같은 멱등키 조합의 중복 Payment insert는 DB 제약으로 한 번 더 방어했습니다.

---

## 핵심 문제와 해결 방식

| 문제                     | 해결 방식                                                     | 검증                                           |
| ---------------------- | --------------------------------------------------------- | -------------------------------------------- |
| 동시 주문 시 재고 초과 판매 가능성   | 상품 재고 조회 구간에 DB 비관적 락 적용                                  | MySQL 환경에서 동시 주문 테스트                         |
| 결제 confirm API 중복 호출   | `Idempotency-Key` 기반 기존 Payment 재사용                       | 같은 멱등키 재요청 시 PG 재조회/후처리 미실행 검증               |
| 같은 주문에 서로 다른 멱등키 동시 요청 | Order row lock으로 Payment 생성 흐름 직렬화                        | 주문 단위 중복 Payment 생성 방지 검증                    |
| 같은 주문/멱등키 중복 insert    | `payment(order_id, idempotency_key)` unique constraint 적용 | DB 레벨 중복 insert 방어                           |
| PG 조회 실패/타임아웃          | `VERIFY_REQUIRED` 상태로 보류 후 보정 배치에서 재조회                    | 성공/실패/금액 불일치/재시도 유지 분기 검증                    |
| PG 결제 금액과 내부 주문 금액 불일치 | 정상 결제 완료 차단 후 PG 전액 취소 시도                                 | 취소 성공 시 `ILLEGAL`, 실패 시 `CANCEL_REQUIRED` 검증 |
| 금액 불일치 취소 실패           | `CANCEL_REQUIRED` 상태로 분리 후 보정 배치에서 취소 재시도                 | 취소 재시도 성공/실패 분기 검증                           |
| 중복 환불 요청               | OrderItem의 `REFUNDED` 상태 검증                               | 재환불 방지 테스트                                   |
| 타 사용자 환불 요청            | 주문 소유자 검증                                                 | 타 사용자 주문 환불 차단 테스트                           |
| 검색 복잡도 과잉              | Elasticsearch 제거 후 QueryDSL 기반 동적 검색으로 단순화                | 상품/쿠폰 조건 검색 테스트                              |

---

## 주요 기능

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
* 결제 성공/실패/보정 상태에 따른 주문 상태 전이 처리

### 결제

* PortOne 결제 승인 정보 조회
* `Idempotency-Key` 기반 결제 confirm 멱등 처리
* 기존 Payment 존재 시 PG 조회 및 결제 후처리 재실행 방지
* Order row lock 기반 Payment 생성 흐름 직렬화
* `payment(order_id, idempotency_key)` unique constraint 기반 중복 insert 방어
* PG 결제 금액과 내부 주문 금액 검증
* 결제 성공 / 실패 / 금액 불일치 / 검증 필요 / 취소 필요 상태 관리
* PG 조회 실패/타임아웃 시 `VERIFY_REQUIRED` 상태로 보류
* 금액 불일치 취소 실패 시 `CANCEL_REQUIRED` 상태로 보류
* 보정 배치 기반 PG 재조회 및 취소 재시도

### 환불

* 환불 요청 및 환불 이력 저장
* `SUCCESS` 상태의 결제만 환불 가능하도록 검증
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

### 1. 결제 confirm 멱등 처리 및 중복 Payment 생성 방지

결제 완료 후 confirm API는 새로고침, 네트워크 재시도, 중복 클릭 등으로 반복 호출될 수 있습니다.

기존에는 `Payment`에 `idempotencyKey` 필드는 있었지만 서버가 매 요청마다 UUID를 생성해 저장했기 때문에, 같은 요청을 식별하는 멱등키 역할을 하지 못했습니다.

이를 개선해 클라이언트가 전달한 `Idempotency-Key`를 저장하고, 같은 `orderId + Idempotency-Key` 요청이 다시 들어오면 기존 `Payment`를 재사용하도록 했습니다.

기존 `Payment`가 있는 경우에는 PG 조회와 결제 후처리 로직을 다시 실행하지 않도록 신규 생성 여부를 분리했습니다.

또한 같은 주문에 대해 서로 다른 `Idempotency-Key` 요청이 동시에 들어오는 경우를 고려해, 새 Payment 생성 전 Order row를 `PESSIMISTIC_WRITE`로 조회하도록 개선했습니다. 락 획득 후 기존 Payment를 다시 조회하고, `OrderStatus.PENDING`인 경우에만 Payment를 생성하도록 해 confirm 생성 흐름을 주문 단위로 직렬화했습니다.

추가로 `payment(order_id, idempotency_key)` unique constraint를 적용해 같은 주문과 같은 멱등키 조합의 중복 insert를 DB 레벨에서도 방어했습니다.

**검증한 내용**

* 같은 `orderId + Idempotency-Key` confirm 재요청 시 기존 Payment 재사용
* 기존 Payment가 있는 confirm 재요청에서는 PG 조회와 후처리 미실행
* 서로 다른 `Idempotency-Key` 요청이 동시에 들어와도 Order row lock으로 Payment 생성 흐름 직렬화
* `payment(order_id, idempotency_key)` unique constraint 기반 중복 insert 방어

---

### 2. PG 조회 실패/타임아웃 보정 상태 처리

외부 PG 조회 실패나 타임아웃이 발생하면 내부 서버는 결제 결과를 즉시 확정할 수 없습니다.

이 상황을 단순 실패로 처리하면 실제로는 PG 결제가 성공했는데 내부 주문만 실패로 남는 정합성 문제가 발생할 수 있습니다.

이를 방지하기 위해 PG 조회 실패나 타임아웃처럼 결제 결과를 확정할 수 없는 경우를 `VERIFY_REQUIRED` 상태로 남기도록 분기했습니다.

이후 PG 재조회에 필요한 `pgPaymentId`를 함께 저장해, 내부 DB 상태와 외부 PG 상태를 나중에 대조할 수 있도록 했습니다.

`VERIFY_REQUIRED` 보정 배치를 추가해 PG 결제 정보를 재조회하고, 조회 결과에 따라 성공, 실패, 금액 불일치, 재시도 유지 상태로 확정하도록 구성했습니다.

**검증한 내용**

* PG 조회 실패/타임아웃 시 `VERIFY_REQUIRED` 상태로 보류
* `VERIFY_REQUIRED` 상태에 `pgPaymentId` 저장
* 보정 배치에서 PG 재조회 후 성공/실패/금액 불일치/재시도 유지 상태로 분기
* 불명확한 결제 상태를 즉시 실패로 단정하지 않고 사후 보정 대상으로 관리

---

### 3. 금액 불일치 결제의 취소 시도 및 보정 처리

PG 승인 금액과 내부 주문 금액이 다르면, 결제가 승인되었더라도 정상 주문으로 처리하면 안 된다고 판단했습니다.

PortOne PG 승인 금액과 내부 주문 금액을 비교해, 두 금액이 일치하지 않는 경우 정상 결제 완료로 처리하지 않도록 했습니다.

금액 불일치가 확인되면 먼저 PortOne 전액 취소를 시도했습니다.

취소가 성공하면 정상 주문으로 보지 않고 `ILLEGAL` 상태로 정리하고, 취소가 실패하면 `CANCEL_REQUIRED` 상태로 남겨 보정 대상으로 분리했습니다.

`CANCEL_REQUIRED` 상태에는 취소 재시도에 필요한 `pgPaymentId`를 저장하고, 보정 배치에서 PG 전액 취소를 재시도하도록 했습니다.

취소 재시도 성공 시 `ILLEGAL`로 정리하고, 실패 시에는 `CANCEL_REQUIRED` 상태를 유지하도록 구성했습니다.

**검증한 내용**

* PG 결제 금액과 내부 주문 금액 불일치 시 정상 결제 완료 차단
* 금액 불일치 결제 취소 성공 시 `ILLEGAL` 처리
* 금액 불일치 결제 취소 실패 시 `CANCEL_REQUIRED` 처리
* `CANCEL_REQUIRED` 보정 배치에서 PG 전액 취소 재시도
* 취소 재시도 성공 시 `ILLEGAL`, 실패 시 `CANCEL_REQUIRED` 유지

---

### 4. 환불 정합성

환불 요청 시 다음 조건을 검증합니다.

* 결제가 `SUCCESS` 상태인지
* 주문이 환불 가능한 상태인지
* 요청 사용자가 주문 소유자인지
* 요청한 OrderItem이 해당 주문에 포함되어 있는지
* 이미 `REFUNDED` 상태인 OrderItem이 포함되어 있지 않은지

이를 통해 타 사용자 주문 환불, 미결제 주문 환불, 중복 환불 요청을 차단했습니다.

**검증한 내용**

* 성공 결제가 아닌 경우 환불 차단
* 다른 사용자의 주문 환불 차단
* 이미 `REFUNDED` 상태인 주문상품에 대한 반복 환불 차단
* 환불 금액 계산 검증

---

### 5. 재고 동시성 처리

한정 수량 상품은 동시에 여러 주문이 들어올 경우 재고 수량보다 많은 주문이 성공할 수 있습니다.

동시성 처리가 없는 상태에서 다수의 동시 주문 테스트를 수행해 Lost Update와 초과 판매 가능성을 확인했습니다.

이후 상품 재고 조회 구간에 DB 비관적 락을 적용해 동일 상품에 대한 재고 차감 요청이 순차 처리되도록 수정했습니다.

비관적 락 적용 후에는 MySQL 환경에서 재고 수량을 초과한 주문이 성공하지 않는지 검증했습니다.

H2 환경에서는 MySQL의 `SELECT FOR UPDATE` 동작을 완전히 보장하기 어려워, 비관적 락 동시성 검증은 MySQL 환경에서 별도로 확인했습니다.

---

### 6. 검색 구조 단순화

상품과 쿠폰 검색은 QueryDSL 기반 동적 검색으로 구현했습니다.

초기에는 Elasticsearch를 실험적으로 검토했지만, 현재 검색 조건은 상품명, 쿠폰명, 가격, 할인율, 만료일 중심입니다.

별도의 검색 엔진을 운영할 만큼의 검색 요구사항이 아니라고 판단해 Elasticsearch 의존성을 제거하고 QueryDSL 검색으로 통일했습니다.

이를 통해 검색 기능은 유지하면서도 운영 복잡도와 관리 포인트를 줄였습니다.

---

## 주요 테스트 케이스

전체 테스트와 빌드는 아래 명령어로 실행합니다.

```bash
./gradlew clean build
```

Windows CMD 환경에서는 아래 명령어를 사용합니다.

```bash
gradlew.bat clean build
```

주요 테스트 케이스는 다음과 같습니다.

* 같은 `orderId + Idempotency-Key` confirm 재요청 시 기존 Payment 재사용 검증
* 기존 Payment가 있는 confirm 재요청에서는 PG 조회와 후처리 미실행 검증
* 서로 다른 `Idempotency-Key` 요청이 동시에 들어오는 경우 Order row lock 기반 Payment 생성 직렬화 검증
* `payment(order_id, idempotency_key)` unique constraint 기반 중복 insert 방어 검증
* PG 조회 실패/타임아웃 시 `VERIFY_REQUIRED` 상태 보류 검증
* `VERIFY_REQUIRED` 보정 배치에서 PG 재조회 후 상태 확정 검증
* PG 결제 금액과 내부 주문 금액 불일치 검증
* 금액 불일치 결제 취소 성공 시 `ILLEGAL` 처리 검증
* 금액 불일치 결제 취소 실패 시 `CANCEL_REQUIRED` 처리 검증
* `CANCEL_REQUIRED` 보정 배치에서 PG 전액 취소 재시도 검증
* 성공 상태가 아닌 결제 환불 차단 검증
* 타 사용자 주문 환불 차단 검증
* 이미 `REFUNDED` 상태인 OrderItem 재환불 차단 검증
* 주문 수량 기반 환불 금액 계산 검증
* 쿠폰 할인 적용 시 주문 총액 계산 검증
* MySQL 환경에서 재고 동시성 검증
* 상품/쿠폰 QueryDSL 동적 검색 검증

일부 동시성 테스트는 MySQL의 `SELECT FOR UPDATE` 동작을 확인하기 위해 로컬 MySQL 환경에서 별도로 검증했습니다.

---

## 의도적으로 단순화한 부분

### Elasticsearch 제거

현재 검색 조건은 상품명, 쿠폰명, 가격, 할인율, 만료일 중심입니다.

복잡한 전문 검색이나 대용량 검색 요구사항이 없는 상태에서 Elasticsearch를 유지하면 운영 복잡도가 커진다고 판단했습니다.

따라서 Elasticsearch 의존성을 제거하고 QueryDSL 기반 동적 검색으로 단순화했습니다.

### Redis / Redisson 제거

재고 차감과 쿠폰 발급 동시성 처리는 DB 비관적 락 기반으로 정리했습니다.

현재 프로젝트는 단일 DB 기반 학습 프로젝트이며, 분산 환경에서 여러 애플리케이션 인스턴스가 동시에 락을 조율해야 하는 요구사항까지는 포함하지 않았습니다.

따라서 Redis/Redisson 기반 분산락은 운영 복잡도 대비 이점이 크지 않다고 판단해 제외했습니다.

### PG 불명확 상태는 실패로 단정하지 않음

PG 조회 실패나 타임아웃은 내부 서버만으로 결제 성공/실패를 확정할 수 없습니다.

따라서 즉시 실패 처리하지 않고 `VERIFY_REQUIRED` 상태로 보류한 뒤, 보정 배치에서 PG 상태를 재조회하도록 설계했습니다.

### 금액 불일치 결제는 정상 주문으로 처리하지 않음

PG 승인 금액과 내부 주문 금액이 다르면 결제 승인이 되었더라도 정상 주문으로 처리하지 않습니다.

먼저 PG 전액 취소를 시도하고, 취소 실패 시 `CANCEL_REQUIRED` 상태로 분리해 재시도 대상으로 관리합니다.

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

* MySQL Testcontainers 기반 동시성 테스트 자동화
* 결제/환불 요청 추적을 위한 요청 식별자 도입
* 보정 배치 실행 이력 및 실패 알림 관리
* 운영 환경을 가정한 관리자용 결제 보정 조회 API
* 부분 환불 정책 고도화
* 재고 차감/복구 이력 테이블 분리
