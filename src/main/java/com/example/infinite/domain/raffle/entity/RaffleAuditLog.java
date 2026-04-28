package com.example.infinite.domain.raffle.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "raffle_audit_logs", indexes = {
        @Index(name = "idx_audit_raffle_id", columnList = "raffle_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RaffleAuditLog {

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

    @Column(nullable = false)
    private Boolean replaced;

    @Column(name = "event_timestamp", nullable = false)
    private Instant eventTimestamp;
}
