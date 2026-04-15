package com.example.infinite.domain.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jelly")
public record JellyProperties(
        int pricePerUnit,       // 1젤리 = 300원
        int fanMembershipCost,  // 팬 멤버십 = 9젤리
        int dmSubscriptionCost  // DM 구독권 = 15젤리
) {
}