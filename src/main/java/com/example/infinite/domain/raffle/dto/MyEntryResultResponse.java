package com.example.infinite.domain.raffle.dto;

import com.example.infinite.domain.raffle.entity.RaffleSlotWinner;
import com.example.infinite.domain.raffle.enums.RewardStatus;

/**
 * 본인 당첨 결과 조회 응답.
 * entered=true이면 응모했고, won=true이면 당첨.
 */
public record MyEntryResultResponse(
        Long raffleId,
        boolean entered,
        boolean won,
        RewardStatus rewardStatus
) {
    public static MyEntryResultResponse notEntered(Long raffleId) {
        return new MyEntryResultResponse(raffleId, false, false, null);
    }

    public static MyEntryResultResponse enteredNotWon(Long raffleId) {
        return new MyEntryResultResponse(raffleId, true, false, null);
    }

    public static MyEntryResultResponse won(Long raffleId, RaffleSlotWinner winner) {
        return new MyEntryResultResponse(raffleId, true, true, winner.getRewardStatus());
    }
}