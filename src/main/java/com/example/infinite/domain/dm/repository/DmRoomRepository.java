package com.example.infinite.domain.dm.repository;

import com.example.infinite.domain.dm.entity.DmRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DmRoomRepository extends JpaRepository<DmRoom, Long> {

    Optional<DmRoom> findByUserIdAndArtistId(Long userId, Long artistId);

    List<DmRoom> findByUserId(Long userId);

    List<DmRoom> findByArtistId(Long artistId);
}
