package com.example.infinite.domain.artistcontent.post.fanletter.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FanLetterRepositoryImpl implements FanLetterRepositoryCustom {

    protected final JPAQueryFactory queryFactory;
}
