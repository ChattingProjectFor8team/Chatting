package com.example.infinite.domain.payment.repository;

import com.example.infinite.domain.payment.entity.JellyTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JellyTransactionRepository extends JpaRepository<JellyTransaction, Long> {

    Page<JellyTransaction> findByUserId(Long userId, Pageable pageable);
}