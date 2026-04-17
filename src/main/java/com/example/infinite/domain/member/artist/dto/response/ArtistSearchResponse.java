package com.example.infinite.domain.member.artist.dto.response;

public record ArtistSearchResponse(
        Long id,
        String name,
        String slug,
        String profileImageUrl
) {
}
