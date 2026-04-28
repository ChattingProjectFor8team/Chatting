package com.example.infinite.domain.payment.dto.response;

import com.example.infinite.domain.payment.entity.AutoChargeHistory;

import java.time.LocalDateTime;

public record AutoChargeHistoryResponse(
        Long id,
        Integer jellyAmount,     // 충전된 젤리 수량
        boolean success,         // 자동충전 성공 여부
        String failReason,       // 실패 사유 (성공 시 null)
        LocalDateTime createdAt  // 충전 시각
) {
    public static AutoChargeHistoryResponse from(AutoChargeHistory history) {
        return new AutoChargeHistoryResponse(
                history.getId(),
                history.getJellyAmount(),
                history.isSuccess(),
                history.getFailReason(),
                history.getCreatedAt()
        );
    }
}