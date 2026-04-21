package com.example.infinite.domain.member.member.repository;

import com.example.infinite.domain.member.member.entity.Member;
import com.example.infinite.domain.member.member.entity.QMember;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepositoryCustom {

    protected final JPAQueryFactory queryFactory;

    @Override
    public Optional<Member> findByNormalizedNickname(String normalizedNickname) {
        if (normalizedNickname == null || normalizedNickname.isBlank()) {
            return Optional.empty();
        }

        QMember member = QMember.member;

        return Optional.ofNullable(queryFactory
                .selectFrom(member)
                .where(member.nickname.lower().eq(normalizedNickname))
                .fetchOne());
    }
}
