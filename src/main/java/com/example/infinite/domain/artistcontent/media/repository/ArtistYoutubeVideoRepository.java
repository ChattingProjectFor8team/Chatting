package com.example.infinite.domain.artistcontent.media.repository;

import com.example.infinite.domain.artistcontent.media.entity.ArtistYoutubeVideo;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArtistYoutubeVideoRepository extends JpaRepository<ArtistYoutubeVideo, Long> {

    boolean existsByArtistIdAndYoutubeVideoId(Long artistId, String youtubeVideoId);

    List<ArtistYoutubeVideo> findByArtistIdOrderByIdDesc(Long artistId, Pageable pageable);

    List<ArtistYoutubeVideo> findByArtistIdAndIdLessThanOrderByIdDesc(Long artistId, Long cursor, Pageable pageable);
}
