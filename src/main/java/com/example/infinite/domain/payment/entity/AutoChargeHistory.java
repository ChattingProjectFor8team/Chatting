package com.example.infinite.domain.payment.entity;

import com.example.infinite.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Entity
@Table(name = "auto_charge_histories",
        indexes = {
                @Index(name = "idx_auto_charge_histories_user_id", columnList = "user_id")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE auto_charge_histories SET deleted_at = current_timestamp WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class AutoChargeHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_key_id", nullable = false)
    private BillingKey billingKey; // 자동충전에 사용된 카드

    @Column(nullable = false)
    private Integer jellyAmount; // 충전된 젤리 수량

    @Column(nullable = false)
    private boolean success; // 자동충전 성공 여부

    @Column
    private String failReason; // 실패 사유 (성공 시 null)

    // PortOne 결제 식별자 — 환불 시 PortOne 취소 API 호출에 사용
    // 이 필드가 null인 이력(이 기능 도입 전 데이터)은 환불 불가
    @Column
    private String portOnePaymentId;

    // 환불 완료 여부 — 젤리 회수 + PortOne 취소가 모두 완료된 경우 true
    // PaymentOrder의 REFUNDED 상태와 동일한 의미이나, AutoChargeHistory는 별도 status 컬럼이 없으므로 플래그로 관리
    @Column(nullable = false)
    private boolean refunded = false;

    @Builder
    public AutoChargeHistory(Long userId, BillingKey billingKey,
                             Integer jellyAmount, boolean success,
                             String failReason, String portOnePaymentId) {
        this.userId = userId;
        this.billingKey = billingKey;
        this.jellyAmount = jellyAmount;
        this.success = success;
        this.failReason = failReason;
        this.portOnePaymentId = portOnePaymentId;
        this.refunded = false;
    }

    // 환불 완료 처리 — RefundService에서 PortOne 취소 + 젤리 회수 완료 후 호출
    public void markRefunded() {
        this.refunded = true;
    }
}