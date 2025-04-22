package kr.hhplus.be.server.application.balance;

import kr.hhplus.be.server.domain.balance.BalanceHistory;
import kr.hhplus.be.server.domain.balance.BalanceHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BalanceHistoryService implements BalanceHistoryUseCase {

    private final BalanceHistoryRepository repository;

    @Override
    public void recordHistory(RecordBalanceHistoryCommand command) {
        BalanceHistory history = BalanceHistory.of(command);
        repository.save(history);
    }
}
