package com.example.infinite.domain.artistcontent.post.artistpost.repository;

import com.example.infinite.domain.artistcontent.post.artistpost.dto.response.ArtistPostReadRow;
import com.example.infinite.domain.artistcontent.post.artistpost.entity.QArtistPost;
import com.example.infinite.domain.member.member.entity.QMember;
import com.example.infinite.global.common.util.querydsl.CursorSliceUtils;
import com.example.infinite.global.common.util.querydsl.QuerydslUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
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

    @Override
    public List<ArtistPostReadRow> findLatestRowsByWriterIds(Collection<Long> writerIds, int limit) {
        QArtistPost artistPost = QArtistPost.artistPost;
        QMember member = QMember.member;

        if (writerIds == null || writerIds.isEmpty()) {
            return List.of();
        }

        /*
         * 이 쿼리는 메인 홈 follow 섹션용 "전역 최신 목록"이다.
         *
         * 중요 포인트:
         * - writerIds 중 누가 썼는지만 본다
         * - 결과는 artist별로 자르지 않고 한 줄의 최신순 피드처럼 정렬한다
         * - 따라서 정렬 기준은 단순 id DESC + limit 이다
         *
         * 반대로 "artist별 최신 n건" 같은 요구에는 이 쿼리를 쓰면 안 되고,
         * 아래 findLatestRowsByArtistIds 처럼 artist 단위 top-N 쿼리가 필요하다.
         */
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
                .join(artistPost.writer, member)
                .where(member.id.in(writerIds))
                .orderBy(CursorSliceUtils.orderByIdDesc(artistPost.id))
                .limit(limit)
                .fetch();
    }

    @Override
    public List<ArtistPostReadRow> findLatestRowsByArtistIds(Collection<Long> artistIds, int perArtistLimit) {
        QArtistPost artistPost = QArtistPost.artistPost;
        QArtistPost newerArtistPost = new QArtistPost("newerArtistPost");
        QMember member = QMember.member;

        if (artistIds == null || artistIds.isEmpty() || perArtistLimit < 1) {
            return List.of();
        }

        /*
         * 이 쿼리는 "artist 여러 개에 대해 각 artist의 최신 n건"을 한 번에 읽기 위한 패턴이다.
         *
         * 어떻게 동작하나?
         * - 바깥 row 를 artistPost 라고 두고
         * - 같은 artist 안에서 자기보다 id 가 더 큰 newerArtistPost 개수를 센다
         * - 그 개수가 perArtistLimit 미만인 row 만 남긴다
         *
         * 예를 들어 perArtistLimit = 2 이면
         * 같은 artist 안에서 "나보다 최신 글이 0개 또는 1개뿐인 row"만 통과한다.
         * 결과적으로 각 artist마다 최신 2건이 남는다.
         *
         * 즉 SQL window function 없이도 Querydsl/JPA 안에서
         * per-group top N 을 표현한 쿼리라고 이해하면 된다.
         */
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
                .join(artistPost.writer, member)
                .where(
                        artistPost.artist.id.in(artistIds),
                        JPAExpressions
                                .select(newerArtistPost.count())
                                .from(newerArtistPost)
                                .where(
                                        newerArtistPost.artist.id.eq(artistPost.artist.id),
                                        newerArtistPost.id.gt(artistPost.id)
                                )
                                .lt((long) perArtistLimit)
                )
                .orderBy(
                        artistPost.artist.id.asc(),
                        CursorSliceUtils.orderByIdDesc(artistPost.id)
                )
                .fetch();
    }
}
