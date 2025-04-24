package kr.hhplus.be.server.domain.balance;


import java.util.List;

public interface BalanceHistoryRepository {
    void save(BalanceHistory history);
    List<BalanceHistory> findAllByUserId(long userId);
    boolean existsByRequestId(String s);
}
