package com.example.infinite.domain.payment.entity;

import com.example.infinite.domain.payment.enums.SubscriptionStatus;
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
@Table(name = "dm_subscriptions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE dm_subscriptions SET deleted_at = current_timestamp WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class DmSubscription extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long artistId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionStatus status;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    @Column(nullable = false)
    private LocalDateTime expiredAt;

    @Builder
    public DmSubscription(Long userId, Long artistId, LocalDateTime startedAt, LocalDateTime expiredAt) {
        this.userId = userId;
        this.artistId = artistId;
        this.status = SubscriptionStatus.ACTIVE;
        this.startedAt = startedAt;
        this.expiredAt = expiredAt;
    }

    public void expire() {
        this.status = SubscriptionStatus.EXPIRED;
    }

    public void cancel() {
        this.status = SubscriptionStatus.CANCELLED;
    }

    public boolean isActive() {
        return this.status == SubscriptionStatus.ACTIVE
                && LocalDateTime.now().isBefore(this.expiredAt);
    }
}