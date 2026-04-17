package com.example.infinite.domain.member.artist.repository;

import com.example.infinite.domain.member.artist.dto.response.ArtistSearchResponse;
import com.example.infinite.domain.member.artist.entity.QArtist;
import com.example.infinite.global.common.querydsl.QuerydslUtils;
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
    public Page<ArtistSearchResponse> searchArtists(String keyword, int size) {
        QArtist artist = QArtist.artist;
        PageRequest pageRequest = PageRequest.of(0, size);

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
                .limit(size)
                .fetch();

        Long total = queryFactory
                .select(artist.count())
                .from(artist)
                .where(where)
                .fetchOne();

        return new PageImpl<>(content, pageRequest, total == null ? 0L : total);
    }
}
