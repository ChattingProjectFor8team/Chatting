package com.example.infinite.domain.artistcontent.post.fanpost.repository;

import com.example.infinite.domain.artistcontent.post.fanpost.dto.response.FanPostReadRow;
import com.example.infinite.domain.artistcontent.post.fanpost.entity.QFanPost;
import com.example.infinite.domain.member.member.entity.QMember;
import com.example.infinite.global.common.util.querydsl.CursorSliceUtils;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class FanPostRepositoryImpl implements FanPostRepositoryCustom {

    protected final JPAQueryFactory queryFactory;

    @Override
    public List<FanPostReadRow> findSliceRowsByArtistId(Long artistId, Long cursor, int limit) {
        QFanPost fanPost = QFanPost.fanPost;
        QMember member = QMember.member;

        // TODO: 구독 도메인 조회가 붙으면 fanMembership/dmSubscription 배지를 배치 조회 후 서비스에서 조립한다.
        return queryFactory
                .select(Projections.constructor(
                        FanPostReadRow.class,
                        fanPost.id,
                        fanPost.artist.id,
                        member.id,
                        member.nickname,
                        member.profileImageUrl,
                        Expressions.constant(Boolean.FALSE),
                        Expressions.constant(Boolean.FALSE),
                        fanPost.content,
                        fanPost.likeCount,
                        fanPost.commentCount,
                        fanPost.mediaCount,
                        fanPost.createdAt
                ))
                .from(fanPost)
                // 작성자 프로필 정보는 목록 응답에서 항상 필요하므로 member를 함께 join한다.
                .join(fanPost.writer, member)
                .where(
                        fanPost.artist.id.eq(artistId),
                        // cursor는 id DESC 무한스크롤 기준으로 "마지막으로 본 글보다 더 오래된 글"만 허용한다.
                        CursorSliceUtils.ltCursor(fanPost.id, cursor)
                )
                // 최신 글 우선 노출 정책을 맞추기 위해 id DESC 정렬을 고정한다.
                .orderBy(CursorSliceUtils.orderByIdDesc(fanPost.id))
                .limit(limit)
                .fetch();
    }

    @Override
    public Optional<FanPostReadRow> findDetailRowByArtistIdAndFanPostId(Long artistId, Long fanPostId) {
        QFanPost fanPost = QFanPost.fanPost;
        QMember member = QMember.member;

        // 상세 조회도 목록과 동일한 projection 축을 써서 DTO 조립 규칙을 통일한다.
        return Optional.ofNullable(queryFactory
                .select(Projections.constructor(
                        FanPostReadRow.class,
                        fanPost.id,
                        fanPost.artist.id,
                        member.id,
                        member.nickname,
                        member.profileImageUrl,
                        Expressions.constant(Boolean.FALSE),
                        Expressions.constant(Boolean.FALSE),
                        fanPost.content,
                        fanPost.likeCount,
                        fanPost.commentCount,
                        fanPost.mediaCount,
                        fanPost.createdAt
                ))
                .from(fanPost)
                .join(fanPost.writer, member)
                .where(
                        fanPost.artist.id.eq(artistId),
                        // fanPostId 조건으로 정확한 상세 1건만 가져온다.
                        fanPost.id.eq(fanPostId)
                )
                .fetchOne());
    }
}
