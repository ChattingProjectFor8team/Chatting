package com.example.infinite.domain.member.member.support;

import com.example.infinite.domain.member.member.entity.Member;
import com.example.infinite.domain.member.member.error.MemberErrorCode;
import com.example.infinite.domain.member.member.error.MemberException;
import com.example.infinite.domain.member.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberReader {

    private final MemberRepository memberRepository;

    // 멤버 조회 실패 시 member 도메인의 not-found 규칙을 공통 적용한다.
    public Member findByEmailOrThrow(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    // ID 기반 조회도 같은 예외 규칙으로 통일한다.
    public Member findByIdOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
    }
}
