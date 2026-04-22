package com.example.infinite.domain.artistcontent.post.fanletter.repository;

import com.example.infinite.domain.artistcontent.post.fanletter.dto.response.FanLetterReadRow;
import com.example.infinite.domain.artistcontent.post.fanletter.dto.response.FanLetterListRow;

import java.util.List;
import java.util.Optional;

public interface FanLetterRepositoryCustom {

    // 카드형 목록에 필요한 조인 결과를 cursor pagination 으로 조회한다.
    List<FanLetterListRow> findSliceRowsByArtistId(Long artistId, Long cursor, int limit);

    // 상세도 목록과 같은 projection 을 재사용해 응답 조립 규칙을 맞춘다.
    Optional<FanLetterReadRow> findDetailRowByArtistIdAndFanLetterId(Long artistId, Long fanLetterId);
}
