package com.example.infinite.domain.Payment.Enums;

public enum TransactionType {
    CHARGE,       // 젤리 충전 (결제)
    USE,          // 젤리 사용 (구독/멤버십 등)
    REWARD,       // 젤리 지급 (이벤트/보상)
    REFUND,       // 환불로 인한 젤리 반환
    EXPIRED       // 유효기간 만료 소멸
}
