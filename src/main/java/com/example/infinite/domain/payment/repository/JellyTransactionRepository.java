package com.example.infinite.domain.payment.repository;

import com.example.infinite.domain.payment.entity.JellyTransaction;
import com.example.infinite.domain.payment.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface JellyTransactionRepository extends JpaRepository<JellyTransaction, Long> {

    Page<JellyTransaction> findByUserId(Long userId, Pageable pageable);

    // 환불 조건 검증용: 특정 시각 이후 USE 트랜잭션이 있으면 젤리를 사용한 것 → 환불 불가
    boolean existsByUserIdAndTypeAndCreatedAtAfter(Long userId, TransactionType type, LocalDateTime after);
}