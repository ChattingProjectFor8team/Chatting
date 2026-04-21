package com.example.infinite.domain.artistcontent.post.artistpost.repository;

import com.example.infinite.domain.artistcontent.post.artistpost.dto.response.ArtistPostReadRow;

import java.util.List;
import java.util.Optional;

public interface ArtistPostRepositoryCustom {
    List<ArtistPostReadRow> findSliceRowsByArtistId(Long artistId, Long cursor, int limit);

    Optional<ArtistPostReadRow> findDetailRowByArtistIdAndArtistPostId(Long artistId, Long artistPostId);
}
