package com.example.infinite.domain.member.artist.dto.request;

import jakarta.validation.constraints.Size;

public record ArtistUpdateRequest(
        @Size(max = 100)
        String name,

        @Size(max = 100)
        String slug,

        @Size(max = 500)
        String profileImageUrl,

        @Size(max = 500)
        String coverImageUrl,

        @Size(max = 5000)
        String intro
) {
}
