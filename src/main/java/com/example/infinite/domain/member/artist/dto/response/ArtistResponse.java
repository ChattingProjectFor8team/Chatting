package com.example.infinite.domain.member.artist.dto.response;

import com.example.infinite.domain.member.member.enums.MemberStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "아티스트 상세 응답")
public record ArtistResponse(
        @Schema(description = "아티스트 ID", example = "1")
        Long artistId,
        @Schema(description = "아티스트 표시 이름", example = "SEVENTEEN")
        String name,
        @Schema(description = "URL 슬러그", example = "seventeen")
        String slug,
        @Schema(description = "대표 프로필 이미지 URL", example = "https://cdn.infinite.com/artists/seventeen/profile.jpg")
        String profileImageUrl,
        @Schema(description = "커버 이미지 URL", example = "https://cdn.infinite.com/artists/seventeen/cover.jpg")
        String coverImageUrl,
        @Schema(description = "소개글", example = "SEVENTEEN 공식 커뮤니티입니다.")
        String intro,
        @Schema(description = "아티스트 상태", example = "ACTIVE")
        MemberStatus artistStatus,
        @Schema(description = "생성 시각", example = "2026-04-19T00:30:00")
        LocalDateTime createdAt,
        @Schema(description = "아티스트에 속한 멤버 목록")
        List<ArtistMemberResponse> artistMembers
) {
    public static ArtistResponse from(List<ArtistDetailRow> rows) {
        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("아티스트 상세 응답을 만들 데이터가 없습니다.");
        }

        ArtistDetailRow first = rows.get(0);
        List<ArtistMemberResponse> artistMembers = rows.stream()
                .filter(row -> row.artistMemberId() != null)
                .map(ArtistMemberResponse::from)
                .toList();

        return new ArtistResponse(
                first.artistId(),
                first.name(),
                first.slug(),
                first.profileImageUrl(),
                first.coverImageUrl(),
                first.intro(),
                first.artistStatus(),
                first.createdAt(),
                artistMembers
        );
    }
}
