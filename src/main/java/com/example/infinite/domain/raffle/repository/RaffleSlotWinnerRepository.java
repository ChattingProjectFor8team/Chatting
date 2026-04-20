package com.example.infinite.domain.raffle.repository;

import com.example.infinite.domain.raffle.entity.RaffleSlotWinner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RaffleSlotWinnerRepository extends JpaRepository<RaffleSlotWinner, Long> {

    Optional<RaffleSlotWinner> findByRaffleIdAndUserId(Long raffleId, Long userId);

    List<RaffleSlotWinner> findByRaffleId(Long raffleId);
}