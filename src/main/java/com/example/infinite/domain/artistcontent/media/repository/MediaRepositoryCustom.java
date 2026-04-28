package com.example.infinite.domain.artistcontent.media.repository;

import com.example.infinite.domain.artistcontent.media.entity.Media;
import com.example.infinite.domain.artistcontent.post.enums.PostType;

import java.util.Collection;
import java.util.List;

public interface MediaRepositoryCustom {

    // 목록 카드용 미리보기는 게시글별 앞쪽 N개만 읽어 over-fetch를 줄인다.
    List<Media> findPreviewByTargetTypeAndTargetIdInOrderByTargetIdAscSortOrderAsc(
            PostType targetType,
            Collection<Long> targetIds,
            int previewLimitPerTarget
    );
}
