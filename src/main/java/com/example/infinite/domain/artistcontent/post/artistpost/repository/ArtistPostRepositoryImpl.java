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
// ArtistPost 조회는 FanPost와 동일하게 "본문 row만 먼저 읽고, media/hashtag는 서비스에서 배치 조립" 구조를 따른다.
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
    public Optional<ArtistPostReadRow> findLatestRowByArtistId(Long artistId) {
        QArtistPost artistPost = QArtistPost.artistPost;
        QMember member = QMember.member;

        // 하이라이트/대시보드는 최신 1건만 필요하므로
        // 목록 10건 slice 대신 최신 row 1건만 바로 읽는다.
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
                        artistPost.mediaCount,
                        artistPost.createdAt
                ))
                .from(artistPost)
                .join(artistPost.writer, member)
                .where(QuerydslUtils.eq(artistPost.artist.id, artistId))
                .orderBy(CursorSliceUtils.orderByIdDesc(artistPost.id))
                .limit(1)
                .fetchOne());
    }

    @Override
    public Optional<ArtistPostReadRow> findDetailRowByArtistIdAndArtistPostId(Long artistId, Long artistPostId) {
        QArtistPost artistPost = QArtistPost.artistPost;
        QMember member = QMember.member;

        // 상세도 목록과 같은 projection을 재사용해 응답 shape 차이를 서비스 조립 쪽으로만 한정한다.
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
