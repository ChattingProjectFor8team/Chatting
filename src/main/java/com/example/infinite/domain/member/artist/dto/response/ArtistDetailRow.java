package com.example.infinite.domain.member.artist.dto.response;

import com.example.infinite.domain.member.member.enums.MemberStatus;

import java.time.LocalDateTime;

// 아티스트 상세와 전체 멤버 목록을 단일 쿼리로 조회
public record ArtistDetailRow(
        Long artistId,
        String name,
        String slug,
        String profileImageUrl,
        String coverImageUrl,
        String intro,
        MemberStatus artistStatus,
        LocalDateTime createdAt,
        Long artistMemberId,
        Long memberId,
        String stageName,
        String artistMemberProfileImageUrl,
        MemberStatus artistMemberStatus,
        Integer sortOrder
) {
}
