package kr.hhplus.be.server.application.balance;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class BalanceFacade {

    private final BalanceRetryService retryService;
    private final BalanceUseCase balanceService;
    private final BalanceHistoryUseCase historyUseCase;

    @Transactional
    public BalanceResult charge(ChargeBalanceCriteria criteria) {
        ChargeBalanceCommand command = ChargeBalanceCommand.from(criteria);

        BalanceInfo info = retryService.chargeWithRetry(command); // 재시도 포함
        historyUseCase.recordHistory(RecordBalanceHistoryCommand.of(criteria));

        return BalanceResult.fromInfo(info);
    }


}

