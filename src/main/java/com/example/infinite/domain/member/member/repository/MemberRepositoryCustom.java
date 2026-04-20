package com.example.infinite.domain.member.member.repository;

import com.example.infinite.domain.member.member.entity.Member;

import java.util.Optional;

// 회원/아티스트 목록 조회처럼 조건이 늘어날 수 있는 쿼리는 여기에 추가한다.
public interface MemberRepositoryCustom {

    Optional<Member> findByNormalizedNickname(String normalizedNickname);
}
