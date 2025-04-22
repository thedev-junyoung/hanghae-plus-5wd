package kr.hhplus.be.server.application.balance;

import kr.hhplus.be.server.common.vo.Money;
import kr.hhplus.be.server.domain.balance.Balance;
import kr.hhplus.be.server.domain.balance.BalanceHistory;
import kr.hhplus.be.server.domain.balance.BalanceHistoryRepository;
import kr.hhplus.be.server.domain.balance.BalanceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class BalanceFacadeIntegrationTest {

    @Autowired
    private BalanceFacade balanceFacade;

    @Autowired
    private BalanceRepository balanceRepository;

    @Autowired
    private BalanceHistoryRepository balanceHistoryRepository;

    @Test
    @DisplayName("초기 잔액 500,000원인 userId=100 유저가 5,000원을 충전하면 잔액은 505,000원이 된다.")
    void charge_success_using_seeded_data() {
        // given
        Long userId = 100L; // 데이터베이스에 이미 존재하는 유저
        Money charge = Money.wons(5_000);

        Balance original = balanceRepository.findByUserId(userId).orElseThrow();
        long beforeAmount = original.getAmount();

        ChargeBalanceCriteria criteria = ChargeBalanceCriteria.of(userId, charge.value(), "충전 테스트");

        // when
        balanceFacade.charge(criteria);

        // then
        Balance updated = balanceRepository.findByUserId(userId).orElseThrow();
        assertThat(updated.getAmount()).isEqualTo(beforeAmount + charge.value());

        BalanceHistory history = balanceHistoryRepository.findAllByUserId(userId).get(0);
        assertThat(history.getAmount()).isEqualTo(charge.value());
        assertThat(history.isChargeHistory()).isTrue();
        assertThat(history.getReason()).isEqualTo("충전 테스트");
    }
}
