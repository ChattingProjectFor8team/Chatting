package com.example.infinite.domain.Payment.Repository;

import com.example.infinite.domain.Payment.Entity.BillingKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BillingKeyRepository extends JpaRepository<BillingKey, Long> {

    List<BillingKey> findByUserId(Long userId);

    Optional<BillingKey> findByUserIdAndDefaultCardTrue(Long userId);
}