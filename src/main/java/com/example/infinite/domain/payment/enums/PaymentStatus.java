package com.example.infinite.domain.payment.enums;

public enum PaymentStatus {
    PENDING,  // 결제 준비 완료, PortOne 결제 대기 중
    PAID,     // 결제 완료, 젤리 지급 완료
    FAILED    // 결제 실패 또는 검증 실패
}
