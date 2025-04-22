package kr.hhplus.be.server.application.balance;

import kr.hhplus.be.server.common.vo.Money;
import kr.hhplus.be.server.domain.balance.Balance;
import kr.hhplus.be.server.domain.balance.BalanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;


/** 동시성 테스트를 위한 통합 테스트 클래스.
 *
 * <p>이 테스트는 여러 사용자가 동시에 잔액 충전 요청을 할 때 발생할 수 있는
 * 동시성 문제를 검증한다.</p>
 *
 * <p>시나리오:</p>
 * <ul>
 *   <li>여러 사용자가 동시에 잔액 충전 요청</li>
 *   <li>각 요청은 10,000원 충전</li>
 * </ul>
 *
 * <p>기대 결과:</p>
 * <ul>
 *   <li>최종 잔액은 요청 수 x 10,000원</li>
 * </ul>
 *
 * <p>⚠️ 해당 테스트는 동시성 처리를 하지 않은 상태에서 실패해야 정상이다.
 */

@SpringBootTest
public class BalanceConcurrencyTest {

    @Autowired
    private BalanceFacade balanceFacade;

    @Autowired
    private BalanceRepository balanceRepository;

    private static final Long USER_ID = 777L;
    private static final int CONCURRENCY = 10;
    private static final long CHARGE_AMOUNT = 10_000L;

    @BeforeEach
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void setUp() {
        Optional<Balance> existing = balanceRepository.findByUserId(USER_ID);

        existing.ifPresentOrElse(
                b -> System.out.println("기존 잔액 존재함"),
                () -> {
                    balanceRepository.save(Balance.createNew(null, USER_ID, Money.wons(0L)));
                    System.out.println("초기 잔액 0원으로 새로 생성됨");
                }
        );
    }

    @Test
    @DisplayName("여러 명이 동시에 충전 요청하면 잔액이 정확히 누적되어야 한다")
    void should_increase_balance_correctly_when_concurrent_charges_happen() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENCY);
        CountDownLatch latch = new CountDownLatch(CONCURRENCY);

        for (int i = 0; i < CONCURRENCY; i++) {
            executor.execute(() -> {
                try {
                    ChargeBalanceCriteria criteria = ChargeBalanceCriteria.of(USER_ID, CHARGE_AMOUNT, "동시성 테스트 충전");
                    balanceFacade.charge(criteria);
                } catch (Exception e) {
                    System.out.println("충전 실패: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        long finalAmount = balanceRepository.findByUserId(USER_ID)
                .orElseThrow(() -> new IllegalStateException("잔액 없음"))
                .getAmount();

        System.out.println("=== 최종 결과 ===");
        System.out.println("예상 잔액: " + (CONCURRENCY * CHARGE_AMOUNT));
        System.out.println("실제 잔액: " + finalAmount);

        assertThat(finalAmount).isEqualTo(CONCURRENCY * CHARGE_AMOUNT);
    }
}
