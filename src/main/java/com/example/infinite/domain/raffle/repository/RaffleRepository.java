package com.example.infinite.domain.raffle.repository;

import com.example.infinite.domain.raffle.entity.Raffle;
import com.example.infinite.domain.raffle.enums.RaffleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RaffleRepository extends JpaRepository<Raffle, Long> {

    List<Raffle> findByStatus(RaffleStatus status);
}