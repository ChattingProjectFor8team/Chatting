package com.example.infinite.domain.artistcontent.interaction.repository;

import com.example.infinite.domain.artistcontent.interaction.entity.Reaction;
import com.example.infinite.domain.artistcontent.interaction.enums.ReactionType;
import com.example.infinite.domain.artistcontent.post.eunms.PostType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InteractionRepository extends JpaRepository<Reaction, Long>, InteractionRepositoryCustom {

    // 반응 중복 여부는 Member(actorId) + target + reactionType 조합으로 판단한다.
    boolean existsByTargetTypeAndTargetIdAndActorIdAndReactionType(
            PostType targetType,
            Long targetId,
            Long actorId,
            ReactionType reactionType
    );

    // 팬레터 특수 좋아요 여부도 같은 Reaction 레코드를 조회한 뒤 actor의 role로 해석한다.
    Optional<Reaction> findByTargetTypeAndTargetIdAndActorIdAndReactionType(
            PostType targetType,
            Long targetId,
            Long actorId,
            ReactionType reactionType
    );

    long countByTargetTypeAndTargetIdAndReactionType(
            PostType targetType,
            Long targetId,
            ReactionType reactionType
    );
}
