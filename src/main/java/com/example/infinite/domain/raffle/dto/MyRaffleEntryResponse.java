package com.example.infinite.domain.raffle.dto;

import java.time.LocalDateTime;

/**
 * 내 응모 내역 목록 항목.
 */
public record MyRaffleEntryResponse(
        Long raffleId,
        String raffleTitle,
        LocalDateTime enteredAt,
        boolean won
) {}