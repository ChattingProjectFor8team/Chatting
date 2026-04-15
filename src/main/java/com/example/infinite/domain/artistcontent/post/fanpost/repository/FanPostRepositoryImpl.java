package com.example.infinite.domain.artistcontent.post.fanpost.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FanPostRepositoryImpl implements FanPostRepositoryCustom {

    protected final JPAQueryFactory queryFactory;
}
