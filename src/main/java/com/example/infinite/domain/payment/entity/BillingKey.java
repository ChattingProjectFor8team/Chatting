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

    // [추가] 대표 카드 설정 - 의도를 명확히 표현하는 메서드명 사용 (@Setter 지양 컨벤션 준수)
    public void markAsDefault() {
        this.defaultCard = true;
    }

    // [추가] 대표 카드 해제 - setDefault(false) 대신 의도 명확한 메서드로 분리
    public void unsetDefault() {
        this.defaultCard = false;
    }
}