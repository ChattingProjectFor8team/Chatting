package com.example.infinite.domain.raffle.entity;

import com.example.infinite.domain.raffle.enums.EntryCondition;
import com.example.infinite.domain.raffle.enums.RaffleStatus;
import com.example.infinite.domain.raffle.enums.RewardType;
import com.example.infinite.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "raffles", indexes = {
        @Index(name = "idx_raffle_artist_status", columnList = "artist_id, status"),
        @Index(name = "idx_raffle_artist_created", columnList = "artist_id, created_at")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE raffles SET deleted_at = current_timestamp WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Raffle extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "artist_id", nullable = false)
    private Long artistId;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_condition", nullable = false, length = 30)
    private EntryCondition entryCondition;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_type", nullable = false, length = 30)
    private RewardType rewardType;

    @Column(name = "total_winners", nullable = false)
    private Integer totalWinners;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RaffleStatus status;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Builder
    private Raffle(Long artistId, String title, EntryCondition entryCondition,
                   RewardType rewardType, Integer totalWinners, Integer durationMinutes) {
        this.artistId = artistId;
        this.title = title;
        this.entryCondition = entryCondition;
        this.rewardType = rewardType;
        this.totalWinners = totalWinners;
        this.durationMinutes = durationMinutes;
        this.status = RaffleStatus.PENDING;
    }

    public void start() {
        this.status = RaffleStatus.ACTIVE;
        this.startedAt = LocalDateTime.now();
    }

    public void complete() {
        this.status = RaffleStatus.COMPLETED;
        this.endedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = RaffleStatus.CANCELED;
        this.endedAt = LocalDateTime.now();
    }
}