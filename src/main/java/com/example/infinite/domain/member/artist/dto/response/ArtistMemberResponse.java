package com.example.infinite.domain.member.artist.dto.response;

import com.example.infinite.domain.member.artist.entity.ArtistMember;
import com.example.infinite.domain.member.member.enums.MemberStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "아티스트 멤버 응답")
public record ArtistMemberResponse(
        @Schema(description = "아티스트 멤버 ID", example = "5")
        Long artistMemberId,
        @Schema(description = "연결된 회원 ID", example = "12")
        Long memberId,
        @Schema(description = "활동명", example = "JEONGHAN")
        String stageName,
        @Schema(description = "프로필 이미지 URL", example = "https://cdn.infinite.com/artists/seventeen/jeonghan.jpg")
        String profileImageUrl,
        @Schema(description = "멤버 상태", example = "ACTIVE")
        MemberStatus status,
        @Schema(description = "정렬 순서", example = "2")
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
