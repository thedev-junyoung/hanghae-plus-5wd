## 💡 동시성 제어 보고서: STEP09

> 서비스 주요 기능의 경쟁 조건(Race Condition) 이슈를 식별하고,
>
>
> RDBMS 기반의 동시성 제어 전략(Optimistic / Pessimistic Lock)을 적용한 사례를 정리합니다.
>

---

## 🧩 주요 시나리오 및 이슈

| 시나리오 | 문제 설명 | 해결 전략 |
| --- | --- | --- |
| **잔액 충전** | 중복 요청으로 인해 과도한 금액 충전 | `Optimistic Lock` + `멱등성` |
| **쿠폰 발급** | 수량 제한 초과 발급 | `Pessimistic Lock` + 중복 발급 차단 |
| **주문** | 동시에 재고 차감 시 재고 초과 판매 발생 | `Pessimistic Lock` |
| **결제** | 하나의 주문에 대해 복수 결제 발생 가능성 | 상태 선점 처리 |

---

## 🔧 적용 전략

### 1. 잔액 충전 (Optimistic Lock + 멱등성)

- `@Version` 필드로 낙관적 락 충돌 감지
- `@Retryable`로 충돌 시 재시도 (`maxAttempts = 5`, `backoff = 100ms`)
- `requestId` 기반 **멱등성 처리**로 중복 충전 차단

```java
public BalanceInfo charge(ChargeBalanceCommand command) {
    if (balanceHistoryRepository.existsByRequestId(command.requestId())) {
        Balance existing = balanceRepository.findByUserId(command.userId()).orElseThrow();
        return BalanceInfo.from(existing); // 중복 요청 방지
    }

    Balance balance = balanceRepository.findByUserId(command.userId())
            .orElseThrow(() -> new BalanceException.NotFoundException(command.userId()));

    balance.charge(Money.wons(command.amount()));
    balanceRepository.save(balance);
    return BalanceInfo.from(balance);
}
```

---

### 2. 쿠폰 발급 (Pessimistic Lock)

- `@Lock(PESSIMISTIC_WRITE)` 쿼리로 **쿠폰 row에 락** 적용
- `remainingQuantity` 검증 및 차감은 트랜잭션 내에서 처리
- `userId + couponId` 조합으로 **중복 발급 차단**

```java

  @Transactional
  public CouponResult issueLimitedCoupon(IssueLimitedCouponCommand command) {
      // 락 걸고 조회
      Coupon coupon = couponRepository.findByCodeForUpdate(command.couponCode());

      // 중복 발급 방지
      if (couponIssueRepository.hasIssued(command.userId(), coupon.getId())) {
          throw new CouponException.AlreadyIssuedException(command.userId(), command.couponCode());
      }

      // 도메인 책임으로 발급 생성 및 수량 차감
      CouponIssue issue = CouponIssue.create(command.userId(), coupon);

      // 저장
      couponIssueRepository.save(issue);

      return CouponResult.from(issue);
  }
```

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT c FROM Coupon c WHERE c.code = :code")
Optional<Coupon> findByCodeForUpdate(@Param("code") String code);
```

---

### 3. 재고 차감 (Pessimistic Lock)

- `@Lock(PESSIMISTIC_WRITE)`로 재고 레코드에 락 적용
- 조회 + 수량 차감은 동일 트랜잭션에서 처리
- 수량 부족 시 예외로 실패 처리

```java
		@Transactional
    public void decrease(DecreaseStockCommand command) {
        ProductStock stock = productStockRepository.findByProductIdAndSizeForUpdate(command.productId(), command.size())
                .orElseThrow(ProductException.InsufficientStockException::new);

        if (stock.getStockQuantity() < command.quantity()) {
            throw new ProductException.InsufficientStockException();
        }

        stock.decreaseStock(command.quantity());

        productStockRepository.save(stock);
    }
```

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT ps FROM ProductStock ps WHERE ps.productId = :productId AND ps.size = :size")
Optional<ProductStock> findByProductIdAndSizeForUpdate(@Param("productId") Long productId, @Param("size") int size);

```

---

### 4. 결제 (주문 상태 선점)

- `@Lock(PESSIMISTIC_WRITE)`로 주문 row 선점
- 결제 요청 시 `CONFIRMED` 여부 확인 후 진행
- 잔액 차감 → 결제 저장 → 주문 상태 변경을 하나의 트랜잭션으로 처리

---

## ✅ 테스트 결과

| 테스트명 | 성공 기준 | 결과 |
| --- | --- | --- |
| `BalanceConcurrencyTest` | 충전 금액이 정확히 누적되어야 함 | ✅ 통과 |
| `CouponConcurrencyTest` | 최대 수량만큼만 발급 | ✅ 통과 |
| `OrderConcurrencyTest` | 재고 초과 주문 차단 | ✅ 통과 |
| `PaymentConcurrencyTest` | 하나의 주문에 대해 1회만 결제 성공 | ✅ 통과 |

---

## 📌 주요 인사이트

- 단순 트랜잭션 처리만으로는 동시성 문제 해결이 불가능
- **락 + 멱등성 + 재시도 + 트랜잭션 경계 설계**의 조합이 중요
- 테스트 시 `@Transactional(propagation = NOT_SUPPORTED)`로 테스트 트랜잭션과 서비스 트랜잭션 분리 필요

---

## 🌀 대안 및 확장 가능성

| 항목 | 제안 |
| --- | --- |
| 분산 환경 대응 | `Redis setNX`를 통한 글로벌 락 고려 |
| 대량 트래픽 대응 | 큐 기반 처리 (Kafka 등)로 전환 가능 |
| 재시도 전략 정교화 | backoff + jitter 도입 고려 |
| 트랜잭션 분리 | 이벤트 기반 히스토리 저장 전략 가능 |

---

## ✅ 결론

- 동시성 처리는 단순한 락 이상의 문제다.
- **성공 기준 정의, 실패 시 처리 전략, 재시도 여부, 트랜잭션 경계 설정**까지 종합적으로 설계해야 한다.
- 실험과 테스트를 통해 실제 문제를 겪어보며 설계를 다듬는 과정이 무엇보다 중요하다.

---

> "동시성은 락의 문제가 아니라, 실패를 설계하는 일이다."
>