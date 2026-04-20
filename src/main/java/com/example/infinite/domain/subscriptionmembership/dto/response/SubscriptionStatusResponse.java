package com.example.infinite.domain.subscriptionmembership.dto.response;

import java.time.LocalDateTime;

public record SubscriptionStatusResponse(
        boolean active,
        LocalDateTime expiredAt
) {
    public static SubscriptionStatusResponse of(boolean active, LocalDateTime expiredAt) {
        return new SubscriptionStatusResponse(active, expiredAt);
    }

    public static SubscriptionStatusResponse inactive() {
        return new SubscriptionStatusResponse(false, null);
    }
}
