package com.example.infinite.domain.payment.dto.response;

import com.example.infinite.domain.payment.entity.AutoChargeSetting;

public record AutoChargeSettingResponse(
        Long id,
        Long billingKeyId,
        String cardName,
        String cardLast4,
        Integer jellyAmount,
        Integer thresholdBalance,
        boolean enabled
) {
    public static AutoChargeSettingResponse from(AutoChargeSetting setting) {
        return new AutoChargeSettingResponse(
                setting.getId(),
                setting.getBillingKey().getId(),
                setting.getBillingKey().getCardName(),
                setting.getBillingKey().getCardLast4(),
                setting.getJellyAmount(),
                setting.getThresholdBalance(),
                setting.isEnabled()
        );
    }
}
