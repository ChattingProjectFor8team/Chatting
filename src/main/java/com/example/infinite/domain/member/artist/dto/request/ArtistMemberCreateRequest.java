package com.example.infinite.domain.member.artist.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ArtistMemberCreateRequest(
        @NotNull
        @Positive
        Long memberId,

        @NotBlank
        @Size(max = 100)
        String stageName,

        @NotBlank
        @Size(max = 500)
        String profileImageUrl,

        @NotNull
        Integer sortOrder
) {
}
