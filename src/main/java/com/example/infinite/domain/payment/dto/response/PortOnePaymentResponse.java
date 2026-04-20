package com.example.infinite.domain.payment.dto.response;

// PortOne GET /payments/{paymentId} 응답 역직렬화용
public record PortOnePaymentResponse(
        String status,  // PAID, FAILED, CANCELLED 등
        String id,
        Amount amount
) {
    public record Amount(int total) {}

    public boolean isPaid() {
        return "PAID".equals(status);
    }
}
