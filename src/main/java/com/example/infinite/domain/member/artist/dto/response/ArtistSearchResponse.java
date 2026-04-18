package com.example.infinite.domain.member.artist.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "아티스트 검색 결과")
public record ArtistSearchResponse(
        @Schema(description = "아티스트 ID", example = "1")
        Long id,
        @Schema(description = "아티스트 표시 이름", example = "SEVENTEEN")
        String name,
        @Schema(description = "URL 슬러그", example = "seventeen")
        String slug,
        @Schema(description = "대표 프로필 이미지 URL", example = "https://cdn.infinite.com/artists/seventeen/profile.jpg")
        String profileImageUrl
) {
}
