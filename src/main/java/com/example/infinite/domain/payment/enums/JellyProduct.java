package com.example.infinite.domain.payment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JellyProduct {
    JELLY_10(10, "젤리 10개"),
    JELLY_50(50, "젤리 50개"),
    JELLY_100(100, "젤리 100개"),
    JELLY_500(500, "젤리 500개");

    private final int jellyAmount;
    private final String orderName;

    // 젤리 수량 × 단가 = 원화 결제 금액
    public int getPrice(int pricePerUnit) {
        return jellyAmount * pricePerUnit;
    }
}
