package com.example.infinite.domain.ArtistContent.Post.FanLetter.Repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FanLetterRepositoryImpl implements FanLetterRepositoryCustom {

    protected final JPAQueryFactory queryFactory;
}
