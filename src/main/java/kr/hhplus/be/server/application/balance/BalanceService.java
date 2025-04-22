package kr.hhplus.be.server.application.balance;

import kr.hhplus.be.server.domain.balance.Balance;
import kr.hhplus.be.server.domain.balance.BalanceException;
import kr.hhplus.be.server.domain.balance.BalanceRepository;
import kr.hhplus.be.server.common.vo.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class BalanceService implements BalanceUseCase {


    private final BalanceRepository balanceRepository;

    @Override
    public BalanceInfo charge(ChargeBalanceCommand command) {
        Balance balance = balanceRepository.findByUserId(command.userId())
                .orElseThrow(() -> new BalanceException.NotFoundException(command.userId()));

        balance.charge(Money.wons(command.amount()));

        Balance updated = balanceRepository.save(balance);
        return BalanceInfo.from(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public BalanceResult getBalance(Long userId) {
        Balance balance = balanceRepository.findByUserId(userId)
                .orElseThrow(() -> new BalanceException.NotFoundException(userId));

        return BalanceResult.fromInfo(BalanceInfo.from(balance));
    }

    @Override
    public boolean decreaseBalance(DecreaseBalanceCommand command) {
        Balance balance = balanceRepository.findByUserId(command.userId())
                .orElseThrow(() -> new BalanceException.NotFoundException(command.userId()));

        balance.decrease(Money.wons(command.amount()));

        balanceRepository.save(balance);

        return true;
    }
}
