package com.example.infinite.domain.artistcontent.post.artistpost.repository;

import com.example.infinite.domain.artistcontent.post.artistpost.dto.response.ArtistPostReadRow;

import java.util.List;
import java.util.Optional;

public interface ArtistPostRepositoryCustom {
    // 메인 커뮤니티 피드용 최신순 cursor slice 조회다.
    List<ArtistPostReadRow> findSliceRowsByArtistId(Long artistId, Long cursor, int limit);

    // 상세 1건도 목록과 같은 projection 축으로 읽어 서비스 조립 규칙을 단순화한다.
    Optional<ArtistPostReadRow> findDetailRowByArtistIdAndArtistPostId(Long artistId, Long artistPostId);
}
