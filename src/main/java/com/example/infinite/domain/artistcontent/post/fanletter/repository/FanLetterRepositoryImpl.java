package com.example.infinite.domain.artistcontent.post.fanletter.repository;

import com.example.infinite.domain.artistcontent.post.fanletter.dto.response.FanLetterReadRow;
import com.example.infinite.domain.artistcontent.post.fanletter.dto.response.FanLetterListRow;
import com.example.infinite.domain.artistcontent.post.fanletter.entity.QFanLetter;
import com.example.infinite.domain.artistcontent.post.fanletter.enums.FanLetterRecipientType;
import com.example.infinite.domain.member.artist.entity.QArtist;
import com.example.infinite.domain.member.artist.entity.QArtistMember;
import com.example.infinite.domain.member.member.entity.QMember;
import com.example.infinite.global.common.util.querydsl.CursorSliceUtils;
import com.example.infinite.global.common.util.querydsl.QuerydslUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class FanLetterRepositoryImpl implements FanLetterRepositoryCustom {

    protected final JPAQueryFactory queryFactory;

    @Override
    public List<FanLetterListRow> findSliceRowsByArtistId(Long artistId, Long cursor, int limit) {
        QFanLetter fanLetter = QFanLetter.fanLetter;
        QArtist artist = QArtist.artist;
        QArtistMember recipientArtistMember = new QArtistMember("recipientArtistMember");
        QMember recipientMember = new QMember("recipientMember");

        return queryFactory
                .select(listProjection(fanLetter, artist, recipientArtistMember, recipientMember))
                .from(fanLetter)
                .join(fanLetter.artist, artist)
                .leftJoin(fanLetter.recipientArtistMember, recipientArtistMember)
                .leftJoin(recipientArtistMember.member, recipientMember)
                .where(
                        QuerydslUtils.eq(artist.id, artistId),
                        CursorSliceUtils.ltCursor(fanLetter.id, cursor)
                )
                .orderBy(CursorSliceUtils.orderByIdDesc(fanLetter.id))
                .limit(limit)
                .fetch();
    }

    @Override
    public Optional<FanLetterReadRow> findDetailRowByArtistIdAndFanLetterId(Long artistId, Long fanLetterId) {
        QFanLetter fanLetter = QFanLetter.fanLetter;
        QArtist artist = QArtist.artist;
        QMember writer = new QMember("writer");
        QArtistMember recipientArtistMember = new QArtistMember("recipientArtistMember");
        QMember recipientMember = new QMember("recipientMember");

        return Optional.ofNullable(queryFactory
                .select(detailProjection(fanLetter, artist, writer, recipientArtistMember, recipientMember))
                .from(fanLetter)
                .join(fanLetter.artist, artist)
                .join(fanLetter.writer, writer)
                .leftJoin(fanLetter.recipientArtistMember, recipientArtistMember)
                .leftJoin(recipientArtistMember.member, recipientMember)
                .where(
                        QuerydslUtils.eq(artist.id, artistId),
                        QuerydslUtils.eq(fanLetter.id, fanLetterId)
                )
                .fetchOne());
    }

    @Override
    public List<FanLetterListRow> findHotRowsByArtistId(
            Long artistId,
            LocalDateTime since,
            int offset,
            int limit,
            Long minLikeCount
    ) {
        QFanLetter fanLetter = QFanLetter.fanLetter;
        QArtist artist = QArtist.artist;
        QArtistMember recipientArtistMember = new QArtistMember("recipientArtistMember");
        QMember recipientMember = new QMember("recipientMember");

        // 목록 카드에 필요한 projection 을 그대로 재사용해
        // HOT 여부와 무관하게 recipient/special-like 조립 방식이 갈라지지 않게 유지한다.
        return queryFactory
                .select(listProjection(fanLetter, artist, recipientArtistMember, recipientMember))
                .from(fanLetter)
                .join(fanLetter.artist, artist)
                .leftJoin(fanLetter.recipientArtistMember, recipientArtistMember)
                .leftJoin(recipientArtistMember.member, recipientMember)
                .where(
                        QuerydslUtils.eq(artist.id, artistId),
                        // 팬레터 HOT도 최근 24시간 제한을 그대로 적용한다.
                        fanLetter.createdAt.goe(since),
                        // FanLetter는 댓글이 없으므로 좋아요 5개 이상만 HOT 후보로 잡는다.
                        QuerydslUtils.goe(fanLetter.likeCount, minLikeCount)
                )
                // FanLetter는 댓글이 없으므로 HOT score를 likeCount 하나로 본다.
                .orderBy(
                        fanLetter.likeCount.desc(),
                        fanLetter.id.desc()
                )
                // 후보군이 이미 필터로 줄어든 목록이라
                // FanLetter HOT은 offset slice 구현을 채택해 FanPost와 비교 학습이 가능하게 둔다.
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    private com.querydsl.core.types.ConstructorExpression<FanLetterListRow> listProjection(
            QFanLetter fanLetter,
            QArtist artist,
            QArtistMember recipientArtistMember,
            QMember recipientMember
    ) {
        // 목록에서는 작성자 정보가 필요 없으므로
        // 수신자 표시와 special-like 오버레이용 artist 정보만 읽는다.
        return Projections.constructor(
                FanLetterListRow.class,
                fanLetter.id,
                fanLetter.recipientType,
                recipientArtistMember.id,
                new CaseBuilder()
                        .when(fanLetter.recipientType.eq(FanLetterRecipientType.ARTIST_MEMBER))
                        .then(recipientArtistMember.stageName)
                        .otherwise(artist.name),
                new CaseBuilder()
                        .when(fanLetter.recipientType.eq(FanLetterRecipientType.ARTIST_MEMBER))
                        .then(recipientArtistMember.profileImageUrl)
                        .otherwise(artist.profileImageUrl),
                artist.name,
                artist.profileImageUrl,
                fanLetter.createdAt
        );
    }

    private com.querydsl.core.types.ConstructorExpression<FanLetterReadRow> detailProjection(
            QFanLetter fanLetter,
            QArtist artist,
            QMember writer,
            QArtistMember recipientArtistMember,
            QMember recipientMember
    ) {
        // 상세는 작성자 기본 정보와 likeCount 까지 함께 읽는다.
        // 수신자 표시용 이름/프로필 분기는 projection 에서 미리 정리해 service 조립을 단순화한다.
        return Projections.constructor(
                FanLetterReadRow.class,
                fanLetter.id,
                artist.id,
                writer.id,
                writer.nickname,
                writer.profileImageUrl,
                fanLetter.recipientType,
                recipientArtistMember.id,
                new CaseBuilder()
                        .when(fanLetter.recipientType.eq(FanLetterRecipientType.ARTIST_MEMBER))
                        .then(recipientArtistMember.stageName)
                        .otherwise(artist.name),
                new CaseBuilder()
                        .when(fanLetter.recipientType.eq(FanLetterRecipientType.ARTIST_MEMBER))
                        .then(recipientArtistMember.profileImageUrl)
                        .otherwise(artist.profileImageUrl),
                artist.name,
                artist.profileImageUrl,
                fanLetter.createdAt
        );
    }
}
