package com.example.infinite.domain.payment.entity;

import com.example.infinite.domain.payment.enums.ReferenceType;
import com.example.infinite.domain.payment.enums.TransactionType;
import com.example.infinite.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "jelly_transactions",
        indexes = {
                @Index(name = "idx_jelly_transactions_user_id", columnList = "user_id"),
                @Index(name = "idx_jelly_transactions_user_type_created", columnList = "user_id, type, created_at")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JellyTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReferenceType referenceType;

    private Long relatedId; // 관련 엔티티 ID (결제ID, 구독ID 등)

    @Column(nullable = false)
    private Integer amount; // 항상 양수, 방향은 type으로 구분

    @Column(nullable = false)
    private Integer balanceAfter; // 거래 후 잔액 스냅샷

    @Builder
    public JellyTransaction(Long userId, TransactionType type,
                            ReferenceType referenceType, Long relatedId,
                            Integer amount, Integer balanceAfter) {
        this.userId = userId;
        this.type = type;
        this.referenceType = referenceType;
        this.relatedId = relatedId;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
    }
}