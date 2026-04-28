package com.example.infinite.domain.artistcontent.post.fanpost.repository;

import com.example.infinite.domain.artistcontent.post.fanpost.dto.response.FanPostReadRow;
import com.example.infinite.domain.artistcontent.post.fanpost.dto.response.FanPostHotReadRow;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FanPostRepositoryCustom {
    List<FanPostReadRow> findSliceRowsByArtistId(Long artistId, Long cursor, int limit);

    Optional<FanPostReadRow> findDetailRowByArtistIdAndFanPostId(Long artistId, Long fanPostId);

    // HOT은 score DESC, id DESC 복합커서 기반이라 latest 목록과 전용 조회 계약을 둔다.
    List<FanPostHotReadRow> findHotSliceRowsByArtistId(
            Long artistId,
            LocalDateTime since,
            Long scoreCursor,
            Long idCursor,
            int limit,
            Long minLikeCount,
            Long minCommentCount
    );
}
