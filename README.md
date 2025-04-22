# **STEP08 - DB & 동시성 테스트 과제**

## 📌 프로젝트 개요

이 과제는 **DB 조회 성능 최적화**와 **동시성 이슈 탐지 및 검증 테스트**를 목표로 합니다.

병목이 발생할 수 있는 조회 쿼리를 실험적으로 재현하고, **인덱스 적용 전후의 실행 계획을 비교 분석**하여 사전 성능 개선 가능성을 검증했습니다.

---

## ✅ 주요 구성

### 1. 📊 **조회 성능 병목 분석 및 인덱스 최적화 보고서**

다음 3가지 API를 대상으로 **실행 계획(Explain Analyze)** 기반의 병목 분석과 인덱스 전략을 수립했습니다:

| 보고서명 | 설명 |
| --- | --- |
| [**popular-products-performance.md**](./report/popular-products-performance.md) | `stat_date + sales_count` 복합 정렬 조건에 대한 병목 분석 |
| [**product-list-created-at-desc-performance.md**](./report/product-list-created-at-desc-performance.md) | 최신순 정렬 + 페이징 (OFFSET, Cursor 기반) 성능 비교 |
| [**product-list-price-sort-performance.md**](./report/product-list-price-sort-performance.md) | 가격 정렬 기준에서 filesort 제거 및 FORCE INDEX 전략 검토 |

각 보고서에는 다음이 포함되어 있습니다:

- 실행 계획 (인덱스 전/후 비교)
- Covering Index, Cursor 방식 적용 결과
- Top-N 정렬 병목 제거 전략
- 실시간 API 기준으로 실무 적용 가능한 개선안

### 2. ⚠️ **동시성 테스트 (Concurrency Tests)**

동시성 테스트는 단위 로직이 아닌, **애플리케이션 레이어의 협력 시나리오를 통해 이슈를 재현**하는 방식으로 구성하였습니다.

각 테스트는 명확한 시나리오를 기반으로 설계되었고, **동시성 제어가 없는 상태에서 실패해야 정상**입니다.

| 테스트명 | 설명 | 링크                                                                                                                |
| --- | --- |-------------------------------------------------------------------------------------------------------------------|
| 주문 재고 차감 | 3명이 동시에 동일 상품을 5개씩 주문 (재고 10개) → 최대 2건 성공 | [**OrderConcurrencyTest**](./src/test/java/kr/hhplus/be/server/application/order/OrderConcurrencyTest.java)       |
| 잔액 충전 | 10명이 동시에 10,000원씩 충전 요청 → 최종 잔액은 100,000원 | [**BalanceConcurrencyTest**](./src/test/java/kr/hhplus/be/server/application/balance/BalanceConcurrencyTest.java) |
| 쿠폰 발급 | 10명이 동시에 2개 한정 쿠폰을 발급 요청 → 초과 발급 여부 확인 | [**CouponConcurrencyTest**](./src/test/java/kr/hhplus/be/server/application/coupon/CouponConcurrencyTest.java)    |

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

- **Testcontainers 기반 통합 테스트**가 실행됩니다.
- DB 설정을 별도로 할 필요 없이 `application.yml` 설정 없이 자동 구성됩니다.
- 모든 테스트는 `@Transactional`로 격리되어 **신뢰 가능한 시뮬레이션 환경**에서 실행됩니다.
