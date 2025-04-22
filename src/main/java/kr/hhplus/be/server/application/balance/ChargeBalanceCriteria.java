package kr.hhplus.be.server.application.balance;

import kr.hhplus.be.server.interfaces.balance.BalanceRequest;

public record ChargeBalanceCriteria(
        Long userId,
        long amount,
        String reason
) {
    public static ChargeBalanceCriteria of(Long userId, long amount, String reason) {
        return new ChargeBalanceCriteria(userId, amount, reason);
    }

    public static ChargeBalanceCriteria fromRequest(BalanceRequest request) {
        return new ChargeBalanceCriteria(
                request.getUserId(),
                request.getAmount(),
                "사용자 요청에 따른 충전"
        );
    }
}
