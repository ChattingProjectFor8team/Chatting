package com.example.infinite.domain.payment.repository;

import com.example.infinite.domain.payment.entity.UserJellyBalance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserJellyBalanceRepository extends JpaRepository<UserJellyBalance, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM UserJellyBalance b WHERE b.userId = :userId")
    Optional<UserJellyBalance> findByUserIdForUpdate(@Param("userId") Long userId);
}