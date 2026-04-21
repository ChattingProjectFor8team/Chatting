package com.example.infinite.domain.realtimelive.repository;

import com.example.infinite.domain.realtimelive.entity.RealtimeLive;
import com.example.infinite.domain.realtimelive.enums.LiveStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RealtimeLiveRepository extends JpaRepository<RealtimeLive, Long> {

    List<RealtimeLive> findByArtistIdOrderByCreatedAtDesc(Long artistId);

    List<RealtimeLive> findByArtistIdAndLiveStatusOrderByCreatedAtDesc(Long artistId, LiveStatus liveStatus);
}
