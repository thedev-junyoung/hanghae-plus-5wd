package kr.hhplus.be.server.application.balance;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Transactional
public class BalanceFacade {

    private final BalanceUseCase balanceUseCase;
    private final BalanceHistoryUseCase historyUseCase;

    public BalanceResult charge(ChargeBalanceCriteria criteria) {
        ChargeBalanceCommand command = ChargeBalanceCommand.from(criteria);

        BalanceInfo info = balanceUseCase.charge(command);

        historyUseCase.recordHistory(
                RecordBalanceHistoryCommand.of(criteria)
        );

        return BalanceResult.fromInfo(info);
    }


}

