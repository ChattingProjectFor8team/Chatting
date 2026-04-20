package com.example.infinite.domain.dm.repository;

import com.example.infinite.domain.dm.entity.DmRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DmRoomRepository extends JpaRepository<DmRoom, Long> {

    Optional<DmRoom> findByUserIdAndArtistId(Long userId, Long artistId);
}