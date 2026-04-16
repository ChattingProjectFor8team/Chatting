package com.example.infinite.domain.payment.dto.response;

import com.example.infinite.domain.payment.entity.AutoChargeSetting;

public record ChargeSettingResponse(
        Long id,
        Long billingKeyId,       // 등록된 카드 ID
        Integer jellyAmount,     // 자동충전 젤리 수량
        Integer thresholdBalance, // 자동충전 기준 잔액
        boolean enabled          // 자동충전 활성 여부
) {
    public static ChargeSettingResponse from(AutoChargeSetting setting) {
        return new ChargeSettingResponse(
                setting.getId(),
                setting.getBillingKey().getId(),
                setting.getJellyAmount(),
                setting.getThresholdBalance(),
                setting.isEnabled()
        );
    }
}