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
@Table(name = "auto_charge_settings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE auto_charge_settings SET deleted_at = current_timestamp WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class AutoChargeSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_key_id", nullable = false)
    private BillingKey billingKey;

    @Column(nullable = false)
    private Integer jellyAmount; // 자동충전 젤리 수량

    @Column(nullable = false)
    private Integer thresholdBalance; // 잔액이 이 값 이하로 떨어지면 자동충전 트리거

    @Column(nullable = false)
    private boolean enabled; // 자동충전 활성 여부

    @Builder
    public AutoChargeSetting(Long userId, BillingKey billingKey,
                             Integer jellyAmount, Integer thresholdBalance) {
        this.userId = userId;
        this.billingKey = billingKey;
        this.jellyAmount = jellyAmount;
        this.thresholdBalance = thresholdBalance;
        this.enabled = true;
    }

    public void update(BillingKey billingKey, Integer jellyAmount, Integer thresholdBalance) {
        this.billingKey = billingKey;
        this.jellyAmount = jellyAmount;
        this.thresholdBalance = thresholdBalance;
    }

    public void disable() {
        this.enabled = false;
    }
}
