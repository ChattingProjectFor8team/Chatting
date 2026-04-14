package com.example.infinite.domain.Payment.Repository;

import com.example.infinite.domain.Payment.Entity.JellyTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JellyTransactionRepository extends JpaRepository<JellyTransaction, Long> {

    Page<JellyTransaction> findByUserId(Long userId, Pageable pageable);
}