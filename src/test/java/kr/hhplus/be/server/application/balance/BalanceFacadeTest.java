package kr.hhplus.be.server.application.balance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BalanceFacadeTest {

    @Mock
    private BalanceUseCase balanceUseCase;

    @Mock
    private BalanceHistoryUseCase historyUseCase;

    @InjectMocks
    private BalanceFacade balanceFacade;

    @Test
    @DisplayName("충전 시 잔액과 이력 업데이트 성공")
    void charge_shouldUpdateBalance_andRecordHistory() {
        // given
        ChargeBalanceCriteria criteria = new ChargeBalanceCriteria(1L, 10000L, "테스트 충전");
        ChargeBalanceCommand command = ChargeBalanceCommand.from(criteria);
        BalanceInfo fakeInfo = new BalanceInfo(1L, 20000L, LocalDateTime.now());
        BalanceResult expectedResult = BalanceResult.fromInfo(fakeInfo);

        // stub
        when(balanceUseCase.charge(command)).thenReturn(fakeInfo);

        // when
        BalanceResult result = balanceFacade.charge(criteria);

        // then
        assertThat(result.userId()).isEqualTo(expectedResult.userId());
        assertThat(result.balance()).isEqualTo(expectedResult.balance());

        verify(balanceUseCase).charge(command);
        verify(historyUseCase).recordHistory(RecordBalanceHistoryCommand.of(criteria));
    }
}
