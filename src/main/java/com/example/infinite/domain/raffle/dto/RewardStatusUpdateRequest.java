package com.example.infinite.domain.raffle.dto;

import com.example.infinite.domain.raffle.enums.RewardStatus;
import jakarta.validation.constraints.NotNull;

public record RewardStatusUpdateRequest(
        @NotNull RewardStatus rewardStatus
) {}