package com.example.infinite.domain.payment.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ChargeSettingRequest(

        @NotNull(message = "빌링키 ID는 필수입니다.")
        Long billingKeyId,          // 사용할 카드 ID

        @NotNull(message = "자동충전 젤리 수량은 필수입니다.")
        @Min(value = 1, message = "최소 1젤리 이상 설정해야 합니다.")
        Integer jellyAmount,        // 자동충전할 젤리 수량

        @NotNull(message = "자동충전 기준 잔액은 필수입니다.")
        @Min(value = 0, message = "기준 잔액은 0 이상이어야 합니다.")
        Integer thresholdBalance    // 잔액이 이 값 이하로 떨어지면 자동충전
) {
}