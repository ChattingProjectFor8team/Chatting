package com.example.infinite.domain.realtimelive.repository;

import com.example.infinite.domain.realtimelive.entity.RealtimeLive;
import com.example.infinite.domain.realtimelive.enums.LiveStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RealtimeLiveRepository extends JpaRepository<RealtimeLive, Long> {

    List<RealtimeLive> findByArtistIdOrderByCreatedAtDesc(Long artistId);

    List<RealtimeLive> findByArtistIdAndLiveStatusOrderByCreatedAtDesc(Long artistId, LiveStatus liveStatus);

    long countByArtistIdAndLiveStatus(Long artistId, LiveStatus liveStatus);

    List<RealtimeLive> findByArtistIdAndLiveStatusOrderByIdDesc(Long artistId, LiveStatus liveStatus, Pageable pageable);

    List<RealtimeLive> findByArtistIdAndLiveStatusAndIdLessThanOrderByIdDesc(
            Long artistId,
            LiveStatus liveStatus,
            Long cursor,
            Pageable pageable
    );
}
