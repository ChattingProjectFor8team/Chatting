package com.example.infinite.domain.member.artist.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ArtistCreateRequest(
        @NotBlank
        @Size(max = 100)
        String name,

        @NotBlank
        @Size(max = 100)
        String slug,

        @NotBlank
        @Size(max = 100)
        String stageName,

        @NotBlank
        @Size(max = 500)
        String profileImageUrl,

        @Size(max = 500)
        String coverImageUrl,

        @Size(max = 5000)
        String intro
) {
}
