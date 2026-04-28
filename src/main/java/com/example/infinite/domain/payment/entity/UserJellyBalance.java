package com.example.infinite.domain.payment.entity;

import com.example.infinite.global.error.ErrorCode;
import com.example.infinite.global.error.PaymentException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "user_jelly_wallet")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserJellyBalance {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false)
    private Integer currentBalance;

    @Version
    private Integer version;

    public UserJellyBalance(Long userId) {
        this.userId = userId;
        this.currentBalance = 0;
    }

    public void charge(int amount) {
        this.currentBalance += amount;
    }

    public void use(int amount) {
        if (this.currentBalance < amount) {
            throw new PaymentException(ErrorCode.JELLY_INSUFFICIENT_BALANCE);
        }
        this.currentBalance -= amount;
    }
}