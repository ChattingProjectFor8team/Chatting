package com.example.infinite.domain.payment.dto.response;

import com.example.infinite.domain.payment.entity.UserJellyBalance;

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