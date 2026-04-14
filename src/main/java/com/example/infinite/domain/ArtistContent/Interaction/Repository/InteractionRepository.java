package com.example.infinite.domain.ArtistContent.Interaction.Repository;

import com.example.infinite.domain.ArtistContent.Interaction.Entity.Reaction;
import com.example.infinite.domain.ArtistContent.Interaction.enums.ReactionType;
import com.example.infinite.domain.ArtistContent.Post.eunms.PostType;
import com.example.infinite.domain.Member.enums.MemberType;
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
