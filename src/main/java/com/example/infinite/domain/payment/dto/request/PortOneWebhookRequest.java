package com.example.infinite.domain.payment.dto.request;

// PortOne v2 웹훅 페이로드
// { "type": "Transaction.Paid", "data": { "paymentId": "...", "transactionId": "..." } }
public record PortOneWebhookRequest(
        String type,
        Data data
) {
    public record Data(String paymentId, String transactionId) {}
}
