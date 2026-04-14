package com.example.infinite.domain.ArtistContent.Interaction.Repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class InteractionRepositoryImpl implements InteractionRepositoryCustom {

    protected final JPAQueryFactory queryFactory;
}
