package com.example.infinite.domain.ArtistContent.Repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ArtistContentRepositoryImpl implements ArtistContentRepositoryCustom {

    protected final JPAQueryFactory queryFactory;
}
