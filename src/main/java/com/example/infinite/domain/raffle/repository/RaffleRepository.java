package com.example.infinite.domain.raffle.repository;

import com.example.infinite.domain.raffle.entity.Raffle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RaffleRepository extends JpaRepository<Raffle, Long> {
}