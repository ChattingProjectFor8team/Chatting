package com.example.infinite.domain.payment.entity;

import com.example.infinite.domain.payment.enums.JellyProduct;
import com.example.infinite.domain.payment.enums.PaymentStatus;
import com.example.infinite.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "payment_orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String paymentId; // PortOne 결제 식별자 (UUID), 웹훅 검증 키

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JellyProduct jellyProduct;

    @Column(nullable = false)
    private int amount; // 원화 결제 금액 (prepare 시점 스냅샷)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column
    private LocalDateTime paidAt;

    @Builder
    public PaymentOrder(String paymentId, Long userId, JellyProduct jellyProduct, int amount) {
        this.paymentId = paymentId;
        this.userId = userId;
        this.jellyProduct = jellyProduct;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
    }

    // 결제·젤리 지급 완료
    public void complete() {
        this.status = PaymentStatus.PAID;
        this.paidAt = LocalDateTime.now();
    }

    // 결제 실패 또는 검증 실패
    public void fail() {
        this.status = PaymentStatus.FAILED;
    }

    // 환불 완료 — PortOne 취소 + 젤리 회수 후 호출
    public void refund() {
        this.status = PaymentStatus.REFUNDED;
    }
}
