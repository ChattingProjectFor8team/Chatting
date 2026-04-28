package com.example.infinite.domain.member.artist.repository;

import com.example.infinite.domain.member.artist.dto.response.ArtistDetailRow;
import com.example.infinite.domain.member.artist.dto.response.ArtistSearchResponse;
import com.example.infinite.domain.member.artist.entity.QArtist;
import com.example.infinite.domain.member.artist.entity.QArtistMember;
import com.example.infinite.global.common.util.querydsl.QuerydslUtils;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

@RequiredArgsConstructor
public class ArtistRepositoryImpl implements ArtistRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<ArtistSearchResponse> searchArtists(String keyword, int page, int size) {
        QArtist artist = QArtist.artist;
        PageRequest pageRequest = PageRequest.of(page, size);

        BooleanBuilder where = new BooleanBuilder()
                .and(QuerydslUtils.likeAnyOf(keyword, artist.name, artist.slug));

        List<ArtistSearchResponse> content = queryFactory
                .select(Projections.constructor(
                        ArtistSearchResponse.class,
                        artist.id,
                        artist.name,
                        artist.slug,
                        artist.profileImageUrl
                ))
                .from(artist)
                .where(where)
                .orderBy(artist.id.desc())
                .offset(pageRequest.getOffset())
                .limit(size)
                .fetch();

        Long total = queryFactory
                .select(artist.count())
                .from(artist)
                .where(where)
                .fetchOne();

        return new PageImpl<>(content, pageRequest, total == null ? 0L : total);
    }

    @Override
    public List<ArtistDetailRow> findArtistDetailRows(Long artistId) {
        QArtist artist = QArtist.artist;
        QArtistMember artistMember = QArtistMember.artistMember;

        // 아티스트 상세와 전체 artist-member 목록을 한 번에 조회해 N+1을 방지한다.
        return queryFactory
                .select(Projections.constructor(
                        ArtistDetailRow.class,
                        artist.id,
                        artist.name,
                        artist.slug,
                        artist.profileImageUrl,
                        artist.coverImageUrl,
                        artist.intro,
                        artist.status,
                        artist.createdAt,
                        artistMember.id,
                        artistMember.member.id,
                        artistMember.stageName,
                        artistMember.profileImageUrl,
                        artistMember.status,
                        artistMember.sortOrder
                ))
                .from(artist)
                .leftJoin(artistMember).on(artistMember.artist.id.eq(artist.id))
                .where(artist.id.eq(artistId))
                .orderBy(artistMember.sortOrder.asc(), artistMember.id.asc())
                .fetch();
    }
}
