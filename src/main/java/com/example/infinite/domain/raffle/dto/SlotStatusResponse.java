package com.example.infinite.domain.raffle.dto;

import com.example.infinite.domain.raffle.entity.RaffleSlot;
import com.example.infinite.domain.raffle.enums.RaffleSlotStatus;

import java.time.LocalDateTime;

/**
 * 관리자용 슬롯 현황 응답.
 */
public record SlotStatusResponse(
        Long slotId,
        int slotIndex,
        RaffleSlotStatus status,
        int targetWinnerCount,
        int carryOverCount,
        LocalDateTime slotStartAt,
        LocalDateTime slotEndAt,
        Long winnerUserId
) {
    public static SlotStatusResponse of(RaffleSlot slot, Long winnerUserId) {
        return new SlotStatusResponse(
                slot.getId(),
                slot.getSlotIndex(),
                slot.getStatus(),
                slot.getTargetWinnerCount(),
                slot.getCarryOverCount(),
                slot.getSlotStartAt(),
                slot.getSlotEndAt(),
                winnerUserId
        );
    }
}