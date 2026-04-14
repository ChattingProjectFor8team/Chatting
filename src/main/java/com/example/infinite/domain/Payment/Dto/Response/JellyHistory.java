package com.example.infinite.domain.Payment.Dto.Response;

import com.example.infinite.domain.Payment.Entity.JellyTransaction;
import com.example.infinite.domain.Payment.Enums.ReferenceType;
import com.example.infinite.domain.Payment.Enums.TransactionType;

import java.time.LocalDateTime;

public record JellyHistory(
        Long id,
        TransactionType type,       // 거래 유형 (CHARGE, USE, REFUND 등)
        ReferenceType referenceType, // 거래 발생 출처 (PAYMENT, DM_SUB, MEMBERSHIP)
        Integer amount,             // 거래 젤리 수량 (항상 양수)
        Integer balanceAfter,       // 거래 후 잔액 스냅샷
        LocalDateTime createdAt
) {
    public static JellyHistory from(JellyTransaction tx) {
        return new JellyHistory(
                tx.getId(),
                tx.getType(),
                tx.getReferenceType(),
                tx.getAmount(),
                tx.getBalanceAfter(),
                tx.getCreatedAt()
        );
    }
}