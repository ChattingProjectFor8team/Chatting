package com.example.infinite.domain.payment.enums;

public enum TransactionType {
    CHARGE,   // 젤리 충전 (결제)
    USE,      // 젤리 사용 (구독/멤버십 등)
    REFUND    // 환불로 인한 젤리 반환
    // REWARD, EXPIRED 는 추후 래플/소멸 기능 추가 시 팀장 확인 후 반영 예정
}
