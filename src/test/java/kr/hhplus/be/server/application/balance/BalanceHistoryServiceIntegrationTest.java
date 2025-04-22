package kr.hhplus.be.server.application.balance;

import kr.hhplus.be.server.domain.balance.BalanceChangeType;
import kr.hhplus.be.server.domain.balance.BalanceHistory;
import kr.hhplus.be.server.domain.balance.BalanceHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@SpringBootTest
@Transactional
class BalanceHistoryServiceIntegrationTest {

    @Autowired
    BalanceHistoryService service;

    @Autowired
    BalanceHistoryRepository repository;

    @Test
    @DisplayName("recordHistory가 호출되면 DB에 히스토리가 저장된다")
    void recordHistory_persistsToDatabase() {
        // given
        Long userId = 100L;
        String reason = "충전 테스트";

        RecordBalanceHistoryCommand command = new RecordBalanceHistoryCommand(
                userId, 5000L, BalanceChangeType.CHARGE, reason
        );

        // when
        service.recordHistory(command);

        // then
        BalanceHistory history = repository.findAllByUserId(userId).get(0);
        assertThat(history.getUserId()).isEqualTo(userId);
        assertThat(history.getAmount()).isEqualTo(5000L);
        assertThat(history.getReason()).isEqualTo(reason);
    }
}
