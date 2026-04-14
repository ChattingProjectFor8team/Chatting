package com.example.infinite.domain.ArtistContent.Post.FanPost.Repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FanPostRepositoryImpl implements FanPostRepositoryCustom {

    protected final JPAQueryFactory queryFactory;
}
