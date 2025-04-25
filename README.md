# **STEP09 - 동시성 제어 구현 과제**

## 📌 프로젝트 개요

이번 과제는 **실제 비즈니스 시나리오 기반의 동시성 이슈를 식별**하고, 이를 해결하기 위해 **낙관적/비관적 락, 멱등성 처리, 재시도 전략 등**을 직접 적용해보는 데 목적이 있습니다.

단순한 트랜잭션 분리 수준을 넘어, **정합성을 보장하면서도 성능을 고려한 현실적인 동시성 처리 전략**을 도입하였습니다.

---

## ✅ 주요 구성

### 1. 🔐 **도메인별 동시성 처리 전략**

| 도메인 | 처리 방식 | 상세 설명 |
| --- | --- | --- |
| **Balance (잔액)** | 낙관적 락 + 멱등성 | `@Version`으로 충돌 감지 + `@Retryable` 재시도 + `requestId`로 중복 방지 |
| **Coupon (쿠폰 발급)** | 비관적 락 | `SELECT FOR UPDATE`로 수량 감소 경쟁 제어, 사용자 중복 발급 방지 |
| **Order (상품 재고)** | 비관적 락 | `SELECT FOR UPDATE`로 재고 차감, 재고 부족 시 즉시 실패 처리 |
| **Payment (결제 중복)** | 비관적 락 | 주문 조회 시 락 선점 후 상태 검증, 한 요청만 결제 성공 처리 |

---

### 2. 🧪 **동시성 테스트**

각 시나리오에 대해 실제 상황을 시뮬레이션하는 **멀티스레드 기반의 통합 테스트**를 구성했습니다.

| 테스트명 | 설명 | 테스트 클래스                                                                                                         |
| --- | --- |-----------------------------------------------------------------------------------------------------------------|
| **잔액 충전** | 10명이 동시에 충전 시 잔액 정확히 누적 | [`BalanceConcurrencyTest`](./src/test/java/kr/hhplus/be/server/application/balance/BalanceConcurrencyTest.java) |
| **쿠폰 발급** | 10명이 2개 한정 쿠폰 요청 → 초과 없음 | [`CouponConcurrencyTest`](./src/test/java/kr/hhplus/be/server/application/coupon/CouponConcurrencyTest.java)    |
| **주문 재고** | 3명이 동시에 5개씩 주문 (재고 10개) → 2건 성공 | [`OrderConcurrencyTest`](./src/test/java/kr/hhplus/be/server/application/order/OrderConcurrencyTest.java)       |
| **결제** | 동일 주문에 여러 결제 시도 → 1건만 성공 | [`PaymentConcurrencyTest`](./src/test/java/kr/hhplus/be/server/application/payment/PaymentConcurrencyTest.java) |

---

### 3. 📃 **보고서 작성**

- [`report/concurrency-control-step09/📝 step09-concurrency-report.md`](./report/concurrency-control-step09/📄%20step09-concurrency-report.md)
  - 문제 식별 → 원인 분석 → 해결 전략 → 테스트 결과 → 한계점까지 포함된 보고서입니다.
  - 락 방식 비교, transaction propagation 전략, 멱등성 처리의 필요성 등 실무 고려 요소들을 정리했습니다.

---

## ⚙️ 실행 방법

### 1. DB 및 초기 데이터 구성

```bash
./init/reset-db.sh
```

- MySQL 컨테이너를 완전히 초기화하고, `init/01-schema.sql`, `02-data.sql`을 자동 적용합니다.

- `./data/mysql` 디렉토리도 함께 초기화되어 **일관된 테스트 조건이 보장**됩니다.

---

### 2. 테스트 실행

```bash
./gradlew test
```

모든 동시성 테스트는 멀티스레드 환경에서 실행됩니다.
@Transactional(propagation = NOT_SUPPORTED) 설정을 통해 테스트 트랜잭션을 명확히 분리합니다.

## 💡 주요 학습 포인트

- DB 락의 특성과 적용 위치의 중요성 (낙관적 vs 비관적)
- 트랜잭션 전파 속성에 따른 설계 전략
- 멱등성과 재시도 처리가 실무에서 중요한 이유
- 테스트 시나리오 설계가 문제 재현 및 해결의 출발점이라는 점

실제로 발생할 수 있는 동시성 문제를 단위가 아닌 흐름 중심으로 설계하고 해결하는 방식은
서비스의 정합성 보장과 운영 안정성 확보 측면에서 큰 인사이트를 제공했습니다.