package com.example.infinite.domain.ArtistContent.Comment.Repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CommentRepositoryImpl implements CommentRepositoryCustom {

    protected final JPAQueryFactory queryFactory;
}
