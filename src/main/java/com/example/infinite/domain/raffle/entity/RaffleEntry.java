package com.example.infinite.domain.raffle.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "raffle_entries", indexes = {
        @Index(name = "uk_raffle_entry_user", columnList = "raffle_id, user_id", unique = true),
        @Index(name = "idx_raffle_entry_user_entered", columnList = "user_id, entered_at")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RaffleEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "raffle_id", nullable = false)
    private Long raffleId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "slot_index", nullable = false)
    private Integer slotIndex;

    @Column(name = "entry_order", nullable = false)
    private Integer entryOrder;

    @Column(name = "entered_at", nullable = false)
    private LocalDateTime enteredAt;

    @Builder
    private RaffleEntry(Long raffleId, Long userId, Integer slotIndex,
                        Integer entryOrder, LocalDateTime enteredAt) {
        this.raffleId = raffleId;
        this.userId = userId;
        this.slotIndex = slotIndex;
        this.entryOrder = entryOrder;
        this.enteredAt = enteredAt;
    }
}