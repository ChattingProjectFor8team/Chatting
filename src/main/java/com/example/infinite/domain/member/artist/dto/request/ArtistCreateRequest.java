package com.example.infinite.domain.member.artist.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "아티스트 생성 요청")
public record ArtistCreateRequest(
        @Schema(description = "아티스트 표시 이름", example = "SEVENTEEN")
        @NotBlank
        @Size(max = 100)
        String name,

        @Schema(description = "URL 식별용 슬러그", example = "seventeen")
        @NotBlank
        // URL 식별자인 slug는 영문/숫자/하이픈 범위만 허용하고 저장 시 소문자로 정규화한다.
        @Pattern(regexp = "^[A-Za-z0-9-]+$")
        @Size(max = 100)
        String slug,

        @Schema(description = "생성자 본인의 활동명", example = "S.COUPS")
        @NotBlank
        @Size(max = 100)
        String stageName,

        @Schema(description = "대표 프로필 이미지 URL", example = "https://cdn.infinite.com/artists/seventeen/profile.jpg")
        @NotBlank
        @Size(max = 500)
        String profileImageUrl,

        @Schema(description = "커버 이미지 URL", example = "https://cdn.infinite.com/artists/seventeen/cover.jpg")
        @Size(max = 500)
        String coverImageUrl,

        @Schema(description = "아티스트 소개", example = "SEVENTEEN 공식 커뮤니티입니다.")
        @Size(max = 5000)
        String intro
) {
}
