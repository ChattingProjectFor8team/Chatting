package com.example.infinite.domain.payment.dto.request;

import jakarta.validation.constraints.NotBlank;

public record BillingKeyRequest(

        @NotBlank(message = "PortOne 빌링키는 필수입니다.")
        String billingKey,   // PortOne에서 프론트가 발급받은 빌링키

        @NotBlank(message = "카드사명은 필수입니다.")
        String cardName,     // 카드사명 (예: 신한, 국민)

        @NotBlank(message = "카드 끝 4자리는 필수입니다.")
        String cardLast4,    // 카드 끝 4자리

        boolean defaultCard  // 대표 카드 등록 여부
) {
}