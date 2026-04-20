package com.example.infinite.domain.raffle.entity;

import com.example.infinite.domain.raffle.enums.RewardStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "raffle_slot_winners", indexes = {
        @Index(name = "uk_raffle_winner", columnList = "raffle_id, user_id", unique = true)
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RaffleSlotWinner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "raffle_id", nullable = false)
    private Raffle raffle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "slot_id", nullable = false)
    private RaffleSlot slot;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "confirmed_at", nullable = false)
    private LocalDateTime confirmedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_status", nullable = false, length = 20)
    private RewardStatus rewardStatus;

    @Column(name = "granted_at")
    private LocalDateTime grantedAt;

    @Builder
    private RaffleSlotWinner(Raffle raffle, RaffleSlot slot, Long userId) {
        this.raffle = raffle;
        this.slot = slot;
        this.userId = userId;
        this.confirmedAt = LocalDateTime.now();
        this.rewardStatus = RewardStatus.PENDING;
    }

    public void grant() {
        this.rewardStatus = RewardStatus.GRANTED;
        this.grantedAt = LocalDateTime.now();
    }

    public void fail() {
        this.rewardStatus = RewardStatus.FAILED;
    }
}