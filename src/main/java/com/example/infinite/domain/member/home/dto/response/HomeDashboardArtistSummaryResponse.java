package com.example.infinite.domain.member.home.dto.response;

import com.example.infinite.domain.member.artist.entity.Artist;

public record HomeDashboardArtistSummaryResponse(
        Long artistId,
        String name,
        String slug,
        String profileImageUrl
) {
    public static HomeDashboardArtistSummaryResponse from(Artist artist) {
        return new HomeDashboardArtistSummaryResponse(
                artist.getId(),
                artist.getName(),
                artist.getSlug(),
                artist.getProfileImageUrl()
        );
    }
}
