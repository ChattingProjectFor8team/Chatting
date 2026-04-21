package com.example.infinite.domain.artistcontent.post.artistpost.repository;

import com.example.infinite.domain.artistcontent.post.artistpost.dto.response.ArtistPostReadRow;
import com.example.infinite.domain.artistcontent.post.artistpost.entity.QArtistPost;
import com.example.infinite.domain.member.member.entity.QMember;
import com.example.infinite.global.common.util.querydsl.CursorSliceUtils;
import com.example.infinite.global.common.util.querydsl.QuerydslUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class ArtistPostRepositoryImpl implements ArtistPostRepositoryCustom {

    protected final JPAQueryFactory queryFactory;

    @Override
    public List<ArtistPostReadRow> findSliceRowsByArtistId(Long artistId, Long cursor, int limit) {
        QArtistPost artistPost = QArtistPost.artistPost;
        QMember member = QMember.member;

        return queryFactory
                .select(Projections.constructor(
                        ArtistPostReadRow.class,
                        artistPost.id,
                        artistPost.artist.id,
                        member.id,
                        member.nickname,
                        member.profileImageUrl,
                        Expressions.constant(Boolean.TRUE),
                        artistPost.content,
                        artistPost.likeCount,
                        artistPost.commentCount,
                        artistPost.mediaCount,
                        artistPost.createdAt
                ))
                .from(artistPost)
                // 작성자 표시는 stageName 이 아니라 실제 Member 닉네임을 사용한다.
                .join(artistPost.writer, member)
                .where(
                        QuerydslUtils.eq(artistPost.artist.id, artistId),
                        CursorSliceUtils.ltCursor(artistPost.id, cursor)
                )
                .orderBy(CursorSliceUtils.orderByIdDesc(artistPost.id))
                .limit(limit)
                .fetch();
    }

    @Override
    public Optional<ArtistPostReadRow> findDetailRowByArtistIdAndArtistPostId(Long artistId, Long artistPostId) {
        QArtistPost artistPost = QArtistPost.artistPost;
        QMember member = QMember.member;

        return Optional.ofNullable(queryFactory
                .select(Projections.constructor(
                        ArtistPostReadRow.class,
                        artistPost.id,
                        artistPost.artist.id,
                        member.id,
                        member.nickname,
                        member.profileImageUrl,
                        Expressions.constant(Boolean.TRUE),
                        artistPost.content,
                        artistPost.likeCount,
                        artistPost.commentCount,
                        artistPost.mediaCount,
                        artistPost.createdAt
                ))
                .from(artistPost)
                .join(artistPost.writer, member)
                .where(
                        QuerydslUtils.eq(artistPost.artist.id, artistId),
                        QuerydslUtils.eq(artistPost.id, artistPostId)
                )
                .fetchOne());
    }
}
