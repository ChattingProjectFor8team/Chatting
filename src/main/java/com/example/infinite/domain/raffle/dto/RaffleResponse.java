package com.example.infinite.domain.raffle.dto;

import com.example.infinite.domain.raffle.entity.Raffle;
import com.example.infinite.domain.raffle.enums.EntryCondition;
import com.example.infinite.domain.raffle.enums.RaffleStatus;
import com.example.infinite.domain.raffle.enums.RewardType;

import java.time.LocalDateTime;

public record RaffleResponse(
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
        LocalDateTime createdAt
) {
    public static RaffleResponse from(Raffle raffle) {
        return new RaffleResponse(
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
                raffle.getCreatedAt()
        );
    }
}