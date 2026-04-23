package com.example.infinite.domain.artistcontent.interaction.repository;

import com.example.infinite.domain.artistcontent.interaction.entity.Reaction;
import com.example.infinite.domain.artistcontent.interaction.enums.ReactionType;
import com.example.infinite.domain.artistcontent.post.enums.PostType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InteractionRepository extends JpaRepository<Reaction, Long>, InteractionRepositoryCustom {

    // 반응 중복 여부는 Member(memberId) + target + reactionType 조합으로 판단한다.
    boolean existsByTargetTypeAndTargetIdAndMemberIdAndReactionType(
            PostType targetType,
            Long targetId,
            Long memberId,
            ReactionType reactionType
    );

    // 팬레터 특수 좋아요 여부도 같은 Reaction 레코드를 조회한 뒤 member의 role로 해석한다.
    Optional<Reaction> findByTargetTypeAndTargetIdAndMemberIdAndReactionType(
            PostType targetType,
            Long targetId,
            Long memberId,
            ReactionType reactionType
    );

    long countByTargetTypeAndTargetIdAndReactionType(
            PostType targetType,
            Long targetId,
            ReactionType reactionType
    );

    // 팬레터 special-like 계산처럼 "여러 대상의 좋아요를 한 번에 읽어야 하는" 배치 조회에 사용한다.
    List<Reaction> findByTargetTypeAndTargetIdInAndReactionType(
            PostType targetType,
            Collection<Long> targetIds,
            ReactionType reactionType
    );
}
