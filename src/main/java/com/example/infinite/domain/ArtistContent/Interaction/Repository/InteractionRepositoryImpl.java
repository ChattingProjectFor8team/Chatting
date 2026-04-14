package com.example.infinite.domain.artistcontent.interaction.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class InteractionRepositoryImpl implements InteractionRepositoryCustom {

    protected final JPAQueryFactory queryFactory;
}
