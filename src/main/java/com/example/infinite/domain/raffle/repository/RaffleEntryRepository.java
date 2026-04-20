package com.example.infinite.domain.raffle.repository;

import com.example.infinite.domain.raffle.entity.RaffleEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RaffleEntryRepository extends JpaRepository<RaffleEntry, Long> {

    Optional<RaffleEntry> findByRaffleIdAndUserId(Long raffleId, Long userId);

    List<RaffleEntry> findByUserIdOrderByEnteredAtDesc(Long userId);

    boolean existsByRaffleIdAndUserId(Long raffleId, Long userId);
}