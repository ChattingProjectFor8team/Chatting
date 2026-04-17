package com.example.infinite.domain.member.member.dto.response;

import com.example.infinite.domain.member.member.entity.Member;

import java.time.LocalDateTime;

// 일반 사용자용 내 정보 응답이다. 내부 권한/상태 정보는 노출하지 않는다.
public record MyInfoResponse(
        Long id,
        String email,
        String nickname,
        String phoneNumber,
        String profileImageUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static MyInfoResponse from(Member member) {
        return new MyInfoResponse(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getPhoneNumber(),
                member.getProfileImageUrl(),
                member.getCreatedAt(),
                member.getUpdatedAt()
        );
    }
}
