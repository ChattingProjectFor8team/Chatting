package com.example.infinite.domain.artistcontent.interaction.repository;

import com.example.infinite.domain.artistcontent.interaction.entity.QReaction;
import com.example.infinite.domain.artistcontent.interaction.enums.ReactionType;
import com.example.infinite.domain.artistcontent.post.enums.PostType;
import com.example.infinite.domain.member.artist.entity.QArtistMember;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

@RequiredArgsConstructor
public class InteractionRepositoryImpl implements InteractionRepositoryCustom {

    protected final JPAQueryFactory queryFactory;

    @Override
    public Set<Long> findTargetIdsReactedByArtistMembers(
            Long artistId,
            PostType targetType,
            Collection<Long> targetIds,
            ReactionType reactionType
    ) {
        if (targetIds == null || targetIds.isEmpty()) {
            return Set.of();
        }

        QReaction reaction = QReaction.reaction;
        QArtistMember artistMember = QArtistMember.artistMember;

        // fan-letter special-like 처럼 "아티스트 멤버가 반응한 target이 있는가"만 필요할 때
        // reaction 전체를 가져오지 않고 targetId 집합만 바로 조회한다.
        return new LinkedHashSet<>(queryFactory
                .select(reaction.targetId)
                .distinct()
                .from(reaction)
                .join(artistMember).on(artistMember.member.id.eq(reaction.memberId))
                .where(
                        artistMember.artist.id.eq(artistId),
                        reaction.targetType.eq(targetType),
                        reaction.reactionType.eq(reactionType),
                        reaction.targetId.in(targetIds)
                )
                .fetch());
    }
}
