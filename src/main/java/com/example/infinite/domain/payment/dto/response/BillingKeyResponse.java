package com.example.infinite.domain.payment.dto.response;

import com.example.infinite.domain.payment.entity.BillingKey;

import java.time.LocalDateTime;

public record BillingKeyResponse(
        Long id,
        String cardName,       // 카드사명
        String cardLast4,      // 카드 끝 4자리
        boolean defaultCard,   // 대표 카드 여부
        LocalDateTime createdAt
) {
    public static BillingKeyResponse from(BillingKey billingKey) {
        return new BillingKeyResponse(
                billingKey.getId(),
                billingKey.getCardName(),
                billingKey.getCardLast4(),
                billingKey.isDefaultCard(),
                billingKey.getCreatedAt()
        );
    }
}