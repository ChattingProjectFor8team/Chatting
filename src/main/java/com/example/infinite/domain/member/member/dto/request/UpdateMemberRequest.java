package com.example.infinite.domain.member.member.dto.request;

// 내 정보 수정에서 사용하는 프로필성 필드 묶음이다.
public record UpdateMemberRequest(
        String nickname,
        String phoneNumber,
        String profileImageUrl
) {
}
