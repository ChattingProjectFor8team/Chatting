package com.example.infinite.domain.artistcontent.post.artistpost.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ArtistPostRepositoryImpl implements ArtistPostRepositoryCustom {

    protected final JPAQueryFactory queryFactory;
}
