package com.example.infinite.domain.member.member.dto.response;

import com.example.infinite.domain.member.member.entity.Member;
import com.example.infinite.domain.member.member.enums.MemberRole;
import com.example.infinite.domain.member.member.enums.MemberStatus;

import java.time.LocalDateTime;

// 관리자용 멤버 응답이다. 운영 판단에 필요한 role/status를 함께 노출한다.
public record AdminMemberResponse(
        Long id,
        String email,
        String nickname,
        String phoneNumber,
        String profileImageUrl,
        MemberRole role,
        MemberStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminMemberResponse from(Member member) {
        return new AdminMemberResponse(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getPhoneNumber(),
                member.getProfileImageUrl(),
                member.getRole(),
                member.getStatus(),
                member.getCreatedAt(),
                member.getUpdatedAt()
        );
    }
}
