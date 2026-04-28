package com.example.infinite.domain.subscriptionmembership.entity;

import com.example.infinite.domain.subscriptionmembership.enums.SubscriptionStatus;
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
@Table(
        name = "dm_subscriptions",
        indexes = {
                @Index(name = "idx_dm_subscriptions_artist_status_user", columnList = "artist_id, status, user_id"),
                @Index(name = "idx_dm_subscriptions_user_id_id", columnList = "user_id, id"),
                @Index(name = "idx_dm_subscriptions_status_expired_at", columnList = "status, expired_at")
        }
)
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

    @Column(nullable = false)
    private int jellyAmount; // 구매 시점 젤리 차감액 스냅샷 (가격 정책 변경 시에도 이력 보존)

    @Builder
    public DmSubscription(Long userId, Long artistId, LocalDateTime startedAt, LocalDateTime expiredAt, int jellyAmount) {
        this.userId = userId;
        this.artistId = artistId;
        this.status = SubscriptionStatus.ACTIVE;
        this.startedAt = startedAt;
        this.expiredAt = expiredAt;
        this.jellyAmount = jellyAmount;
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

    /**
     * 만료일을 지정 일수만큼 연장한다.
     * 래플 당첨 보상(MEMBERSHIP_EXTENSION) 등에서 사용.
     */
    public void extendExpiry(int days) {
        this.expiredAt = this.expiredAt.plusDays(days);
    }
}
