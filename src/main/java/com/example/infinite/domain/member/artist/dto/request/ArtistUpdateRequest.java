package com.example.infinite.domain.member.artist.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "아티스트 수정 요청")
public record ArtistUpdateRequest(
        @Schema(description = "수정할 아티스트 표시 이름", example = "SEVENTEEN")
        @Size(max = 100)
        String name,

        @Schema(description = "수정할 URL 슬러그", example = "seventeen")
        // 부분 수정에서도 slug는 URL 안전 문자만 허용한다.
        @Pattern(regexp = "^[A-Za-z0-9-]+$")
        @Size(max = 100)
        String slug,

        @Schema(description = "수정할 대표 프로필 이미지 URL", example = "https://cdn.infinite.com/artists/seventeen/profile-v2.jpg")
        @Size(max = 500)
        String profileImageUrl,

        @Schema(description = "수정할 커버 이미지 URL", example = "https://cdn.infinite.com/artists/seventeen/cover-v2.jpg")
        @Size(max = 500)
        String coverImageUrl,

        @Schema(description = "수정할 소개글", example = "SEVENTEEN 공식 커뮤니티와 최신 소식을 확인하세요.")
        @Size(max = 5000)
        String intro
) {
}
