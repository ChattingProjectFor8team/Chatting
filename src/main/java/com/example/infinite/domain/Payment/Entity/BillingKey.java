package com.example.infinite.domain.Payment.Entity;

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
@Table(name = "billing_keys")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE billing_keys SET deleted_at = current_timestamp WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class BillingKey extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String billingKey; // PortOne에서 발급된 빌링키

    @Column(nullable = false, length = 20)
    private String cardName; // 카드사명 (예: 신한, 국민)

    @Column(nullable = false, length = 10)
    private String cardLast4; // 카드 끝 4자리

    @Column(nullable = false)
    private boolean defaultCard; // 대표 카드 여부

    @Builder
    public BillingKey(Long userId, String billingKey,
                      String cardName, String cardLast4, boolean defaultCard) {
        this.userId = userId;
        this.billingKey = billingKey;
        this.cardName = cardName;
        this.cardLast4 = cardLast4;
        this.defaultCard = defaultCard;
    }

    public void setDefault(boolean defaultCard) {
        this.defaultCard = defaultCard;
    }
}