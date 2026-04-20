package com.example.infinite.domain.raffle.dto;

import com.example.infinite.domain.raffle.enums.EntryCondition;
import com.example.infinite.domain.raffle.enums.RewardType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 래플 생성 요청.
 * rewardType은 현재 MVP에서 MEMBERSHIP_EXTENSION만 허용.
 * 향후 CHAT_TICKET, SIGNED_PHOTO, VIDEO_CALL 등 확장 시 서버 검증 로직만 수정하면 된다.
 */
public record CreateRaffleRequest(
        @NotBlank String title,
        @Min(1) int totalWinners,
        @Min(1) int durationMinutes,
        @NotNull EntryCondition entryCondition,
        @NotNull RewardType rewardType
) {}