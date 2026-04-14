package com.example.infinite.domain.Payment.Dto.Response;

import com.example.infinite.domain.Payment.Entity.UserJellyBalance;

public record JellyBalance(
        Long userId,
        Integer currentBalance
) {
    public static JellyBalance from(UserJellyBalance balance) {
        return new JellyBalance(
                balance.getUserId(),
                balance.getCurrentBalance()
        );
    }
}