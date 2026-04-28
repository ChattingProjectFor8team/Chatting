package com.example.infinite.domain.member.artist.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "아티스트 멤버 생성 요청")
public record ArtistMemberCreateRequest(
        @Schema(description = "추가할 회원 ID", example = "12")
        @NotNull
        @Positive
        Long memberId,

        @Schema(description = "아티스트 내 활동명", example = "JEONGHAN")
        @NotBlank
        @Size(max = 100)
        String stageName,

        @Schema(description = "멤버 프로필 이미지 URL", example = "https://cdn.infinite.com/artists/seventeen/jeonghan.jpg")
        @NotBlank
        @Size(max = 500)
        String profileImageUrl,

        @Schema(description = "멤버 노출 순서", example = "2")
        @NotNull
        @Positive
        Integer sortOrder
) {
}
