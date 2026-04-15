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
@Table(name = "auto_charge_histories")
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

    @Builder
    public AutoChargeHistory(Long userId, BillingKey billingKey,
                             Integer jellyAmount, boolean success, String failReason) {
        this.userId = userId;
        this.billingKey = billingKey;
        this.jellyAmount = jellyAmount;
        this.success = success;
        this.failReason = failReason;
    }
}