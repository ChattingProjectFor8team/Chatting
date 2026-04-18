package com.example.infinite.domain.member.artist.dto.response;

import com.example.infinite.domain.member.artist.entity.ArtistMember;
import com.example.infinite.domain.member.member.enums.MemberStatus;

public record ArtistMemberResponse(
        Long artistMemberId,
        Long memberId,
        String stageName,
        String profileImageUrl,
        MemberStatus status,
        Integer sortOrder
) {
    public static ArtistMemberResponse from(ArtistDetailRow row) {
        return new ArtistMemberResponse(
                row.artistMemberId(),
                row.memberId(),
                row.stageName(),
                row.artistMemberProfileImageUrl(),
                row.artistMemberStatus(),
                row.sortOrder()
        );
    }

    public static ArtistMemberResponse from(ArtistMember artistMember) {
        return new ArtistMemberResponse(
                artistMember.getId(),
                artistMember.getMember().getId(),
                artistMember.getStageName(),
                artistMember.getProfileImageUrl(),
                artistMember.getStatus(),
                artistMember.getSortOrder()
        );
    }
}
