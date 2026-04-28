package com.example.infinite.domain.raffle.dto;

import com.example.infinite.domain.raffle.entity.Raffle;
import com.example.infinite.domain.raffle.enums.EntryCondition;
import com.example.infinite.domain.raffle.enums.RaffleStatus;
import com.example.infinite.domain.raffle.enums.RewardType;

import java.time.LocalDateTime;

/**
 * 사용자용 래플 상세 조회 응답. 본인 응모 여부 포함.
 */
public record RaffleDetailResponse(
        Long id,
        Long artistId,
        String title,
        EntryCondition entryCondition,
        RewardType rewardType,
        int totalWinners,
        int durationMinutes,
        RaffleStatus status,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        boolean entered
) {
    public static RaffleDetailResponse of(Raffle raffle, boolean entered) {
        return new RaffleDetailResponse(
                raffle.getId(),
                raffle.getArtistId(),
                raffle.getTitle(),
                raffle.getEntryCondition(),
                raffle.getRewardType(),
                raffle.getTotalWinners(),
                raffle.getDurationMinutes(),
                raffle.getStatus(),
                raffle.getStartedAt(),
                raffle.getEndedAt(),
                entered
        );
    }
}