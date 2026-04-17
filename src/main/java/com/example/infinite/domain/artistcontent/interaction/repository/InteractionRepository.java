package com.example.infinite.domain.artistcontent.interaction.repository;

import com.example.infinite.domain.artistcontent.interaction.entity.Reaction;
import com.example.infinite.domain.artistcontent.interaction.enums.ReactionType;
import com.example.infinite.domain.artistcontent.post.eunms.PostType;
import com.example.infinite.domain.member.member.enums.MemberType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InteractionRepository extends JpaRepository<Reaction, Long>, InteractionRepositoryCustom {

    boolean existsByTargetTypeAndTargetIdAndActorTypeAndActorIdAndReactionType(
            PostType targetType,
            Long targetId,
            MemberType actorType,
            Long actorId,
            ReactionType reactionType
    );

    Optional<Reaction> findByTargetTypeAndTargetIdAndActorTypeAndActorIdAndReactionType(
            PostType targetType,
            Long targetId,
            MemberType actorType,
            Long actorId,
            ReactionType reactionType
    );

    long countByTargetTypeAndTargetIdAndReactionType(
            PostType targetType,
            Long targetId,
            ReactionType reactionType
    );
}
