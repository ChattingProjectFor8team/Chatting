package com.example.infinite.domain.raffle.repository;

import com.example.infinite.domain.raffle.entity.Raffle;
import com.example.infinite.domain.raffle.entity.RaffleSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RaffleSlotRepository extends JpaRepository<RaffleSlot, Long> {

    List<RaffleSlot> findByRaffleOrderBySlotIndex(Raffle raffle);
}