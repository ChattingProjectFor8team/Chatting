package com.example.infinite.domain.member.artist.dto.response;

public record ArtistPopularSearchResponse(
        String keyword,
        long score
) {
}
