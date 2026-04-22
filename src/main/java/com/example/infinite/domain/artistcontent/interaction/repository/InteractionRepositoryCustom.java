package com.example.infinite.domain.artistcontent.interaction.repository;

import com.example.infinite.domain.artistcontent.interaction.enums.ReactionType;
import com.example.infinite.domain.artistcontent.post.eunms.PostType;

import java.util.Collection;
import java.util.Set;

public interface InteractionRepositoryCustom {

    // 아티스트 소속 멤버가 반응한 targetId 집합만 바로 뽑아
    // 서비스에서 전체 reaction row를 다시 필터링하지 않게 한다.
    Set<Long> findTargetIdsReactedByArtistMembers(
            Long artistId,
            PostType targetType,
            Collection<Long> targetIds,
            ReactionType reactionType
    );
}
