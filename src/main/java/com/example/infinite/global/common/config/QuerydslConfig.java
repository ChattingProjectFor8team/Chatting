package com.example.infinite.global.common.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
// Querydsl 조회 로직에서 공통으로 사용할 JPAQueryFactory를 등록한다.
public class QuerydslConfig {

    @PersistenceContext
    private EntityManager em;

    @Bean
    public JPAQueryFactory queryFactory() {
        // EntityManager를 감싼 Querydsl 진입점을 스프링 빈으로 제공한다.
        return new JPAQueryFactory(em);
    }
}
