package com.example.infinite.domain.raffle.entity;

import com.example.infinite.domain.raffle.enums.RaffleSlotStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "raffle_slots", indexes = {
        @Index(name = "uk_raffle_slot", columnList = "raffle_id, slot_index", unique = true)
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RaffleSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "raffle_id", nullable = false)
    private Raffle raffle;

    @Column(name = "slot_index", nullable = false)
    private Integer slotIndex;

    @Column(name = "slot_start_at")
    private LocalDateTime slotStartAt;

    @Column(name = "slot_end_at")
    private LocalDateTime slotEndAt;

    @Column(name = "target_winner_count", nullable = false)
    private Integer targetWinnerCount;

    @Column(name = "carry_over_count", nullable = false)
    private Integer carryOverCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RaffleSlotStatus status;

    @Builder
    private RaffleSlot(Raffle raffle, Integer slotIndex, Integer targetWinnerCount) {
        this.raffle = raffle;
        this.slotIndex = slotIndex;
        this.targetWinnerCount = targetWinnerCount;
        this.carryOverCount = 0;
        this.status = RaffleSlotStatus.WAITING;
    }

    /**
     * 래플 시작(start) 시점에 호출. PENDING 상태에서는 시각이 null이다.
     */
    public void fillTimes(LocalDateTime startAt, LocalDateTime endAt) {
        this.slotStartAt = startAt;
        this.slotEndAt = endAt;
    }

    public void activate() {
        this.status = RaffleSlotStatus.ACTIVE;
    }

    public void complete() {
        this.status = RaffleSlotStatus.COMPLETED;
    }

    public void markEmpty(int carryOver) {
        this.status = RaffleSlotStatus.EMPTY;
        this.carryOverCount = carryOver;
    }

    public void addCarryOver(int count) {
        this.targetWinnerCount += count;
        this.carryOverCount += count;
    }
}