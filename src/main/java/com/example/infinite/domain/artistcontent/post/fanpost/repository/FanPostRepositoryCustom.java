package com.example.infinite.domain.artistcontent.post.fanpost.repository;

import com.example.infinite.domain.artistcontent.post.fanpost.dto.response.FanPostReadRow;

import java.util.List;
import java.util.Optional;

public interface FanPostRepositoryCustom {
    List<FanPostReadRow> findSliceRowsByArtistId(Long artistId, Long cursor, int limit);

    Optional<FanPostReadRow> findDetailRowByArtistIdAndFanPostId(Long artistId, Long fanPostId);
}
