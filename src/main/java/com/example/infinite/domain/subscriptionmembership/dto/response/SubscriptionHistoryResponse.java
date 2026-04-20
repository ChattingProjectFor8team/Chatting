package com.example.infinite.domain.subscriptionmembership.dto.response;

import com.example.infinite.domain.subscriptionmembership.entity.DmSubscription;
import com.example.infinite.domain.subscriptionmembership.entity.FanMembership;
import com.example.infinite.domain.subscriptionmembership.enums.SubscriptionStatus;

import java.time.LocalDateTime;

public record SubscriptionHistoryResponse(
        Long id,
        Long artistId,
        SubscriptionStatus status,
        LocalDateTime startedAt,
        LocalDateTime expiredAt
) {
    public static SubscriptionHistoryResponse from(DmSubscription sub) {
        return new SubscriptionHistoryResponse(
                sub.getId(),
                sub.getArtistId(),
                sub.getStatus(),
                sub.getStartedAt(),
                sub.getExpiredAt()
        );
    }

    public static SubscriptionHistoryResponse from(FanMembership membership) {
        return new SubscriptionHistoryResponse(
                membership.getId(),
                membership.getArtistId(),
                membership.getStatus(),
                membership.getStartedAt(),
                membership.getExpiredAt()
        );
    }
}
