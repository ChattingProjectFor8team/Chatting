package com.example.infinite.domain.artistcontent.post.artistpost.repository;

import com.example.infinite.domain.artistcontent.post.artistpost.dto.response.ArtistPostReadRow;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ArtistPostRepositoryCustom {
    // 메인 커뮤니티 피드용 최신순 cursor slice 조회다.
    List<ArtistPostReadRow> findSliceRowsByArtistId(Long artistId, Long cursor, int limit);

    // 대시보드처럼 "최신 1건만 필요할 때" 목록 10개를 통째로 읽지 않기 위한 전용 조회다.
    Optional<ArtistPostReadRow> findLatestRowByArtistId(Long artistId);

    // 상세 1건도 목록과 같은 projection 축으로 읽어 서비스 조립 규칙을 단순화한다.
    Optional<ArtistPostReadRow> findDetailRowByArtistIdAndArtistPostId(Long artistId, Long artistPostId);

    // 홈 대시보드의 "팔로우한 멤버 최신 글" 섹션은 writer 기준 전역 최신 몇 건만 필요하다.
    List<ArtistPostReadRow> findLatestRowsByWriterIds(Collection<Long> writerIds, int limit);

    // 홈 대시보드의 "구독한 아티스트별 최신 n건" 섹션은 artist별 상위 몇 건을 한 번에 읽는다.
    List<ArtistPostReadRow> findLatestRowsByArtistIds(Collection<Long> artistIds, int perArtistLimit);
}
