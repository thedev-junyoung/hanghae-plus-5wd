# STEP09 - Concurrency Report

> 본 보고서는 서비스 내 주요 기능에서 발생할 수 있는 동시성 문제를 식별하고,
RDBMS 기반의 동시성 제어 방식(Optimistic / Pessimistic Lock)을 통해 이를 해결한 과정을 정리합니다.
>

---

## 1. 문제 식별 (Context & Issue)

### 1.1. 주요 시나리오 및 예상 이슈

| 시나리오 | 예상 문제 | 설명 |
| --- | --- | --- |
| 잔액 충전 | 중복 충전, Race Condition | 동시에 동일한 유저가 잔액을 충전할 경우 누적 이상 발생 가능 |
| 쿠폰 발급 | 초과 발급 | 선착순 제한 쿠폰에서 동시 요청 시 발급 수량 초과 가능성 |
| 상품 재고 차감 | 음수 재고 | 다수 주문 요청이 동시에 들어올 경우 재고가 음수가 될 수 있음 |
| 결제 요청 | 중복 결제 | 동일 주문에 대해 여러 결제 시도가 발생할 수 있음 |

---

## 2. 분석 (AS-IS)

| 시나리오 | 동시성 방어 없음 시 문제 | 테스트 결과 |
| --- | --- | --- |
| 잔액 충전 | 중복 커밋 발생 → 잔액 초과 | 실제 테스트에서 `@Transactional`만 사용 시 100,000원이 530,000원까지 증가 |
| 쿠폰 발급 | 수량 제한 무시하고 초과 발급 발생 | 동시 요청 시 100개 중 105건 발급 확인 |
| 재고 차감 | `재고 >= 수량` 체크 이전에 충돌 발생 | 재고가 음수로 내려감 |
| 결제 요청 | 중복 결제 로직이 여러 번 실행됨 | 주문 상태가 여러 번 변경되거나, 잔액이 중복 차감됨 |

---

## 3. 해결 방안 (TO-BE 설계)

### 3.1. 잔액 충전 - Optimistic Lock + Retry

- `@Version` 필드 활용하여 충돌 감지
- `@Retryable`로 재시도 로직 구성
- `@Transactional(REQUIRES_NEW)`로 트랜잭션 분리
- `requestId`로 **idempotency** 보장

```java
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BalanceInfo charge(ChargeBalanceCommand command) {
        if (balanceHistoryRepository.existsByRequestId(command.requestId())) {
            log.warn("이미 처리된 충전 요청입니다: userId={}, requestId={}", command.userId(), command.requestId());
            Balance existing = balanceRepository.findByUserId(command.userId()).orElseThrow();
            return BalanceInfo.from(existing); // 이전 충전 결과 그대로 반환
        }

        Balance balance = balanceRepository.findByUserId(command.userId())
                .orElseThrow(() -> new BalanceException.NotFoundException(command.userId()));

        balance.charge(Money.wons(command.amount()));
        balanceRepository.save(balance);

        return BalanceInfo.from(balance);
    }

```

### 3.2. 쿠폰 발급 - Pessimistic Lock (FOR UPDATE)

`CouponRepository.findByCodeForUpdate(...)`

수량 차감 로직과 함께 트랜잭션 묶음

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

### 3.3. 재고 차감 - Pessimistic Lock (FOR UPDATE)

`ProductStockRepository.findByProductIdAndSizeForUpdate(...)`

수량 차감 트랜잭션에 묶어 음수 방지

### 3.4. 결제 요청 - 중복 방지 로직

- 주문 상태 확인 후 처리
- 충돌 시 예외 발생 → 재시도 하지 않음
- 잔액 차감/결제 기록/주문 상태 변경을 단일 트랜잭션으로 묶음

## 4. 테스트 및 검증

✅ 동시성 테스트 100회 시도에서 잔액 초과 없음, 중복 발급 없음, 재고 음수 없음 확인
`@Retryable`, `@Transactional` 전파 수준 조정으로 문제 해결

`BalanceServiceIntegrationTest`에서 `@Transactional(propagation = NOT_SUPPORTED)`으로 테스트 트랜잭션 분리하여 `REQUIRES_NEW` 정상 반영
