package com.example.infinite.domain.payment.dto.response;

// 결제 준비 응답 — 클라이언트가 PortOne SDK에 paymentId와 amount를 전달해 결제 진행
public record PaymentPrepareResponse(
        String paymentId,   // PortOne SDK에 전달할 결제 식별자
        String orderName,   // PortOne 결제창에 표시할 상품명
        int jellyAmount,    // 지급될 젤리 수량
        int amount          // 원화 결제 금액
) {}
