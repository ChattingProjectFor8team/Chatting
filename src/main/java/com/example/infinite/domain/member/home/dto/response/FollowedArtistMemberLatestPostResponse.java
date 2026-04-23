package com.example.infinite.domain.member.home.dto.response;

import com.example.infinite.domain.artistcontent.post.artistpost.dto.response.ArtistPostResponse;
import com.example.infinite.domain.member.artist.entity.ArtistMember;

// 팔로우 대상은 ArtistMember 이므로, 글 본문 외에 stageName/profile 스냅샷도 함께 내려준다.
public record FollowedArtistMemberLatestPostResponse(
        Long artistMemberId,
        HomeDashboardArtistSummaryResponse artist,
        String stageName,
        String profileImageUrl,
        ArtistPostResponse post
) {
    public static FollowedArtistMemberLatestPostResponse from(
            ArtistMember artistMember,
            ArtistPostResponse post
    ) {
        return new FollowedArtistMemberLatestPostResponse(
                artistMember.getId(),
                HomeDashboardArtistSummaryResponse.from(artistMember.getArtist()),
                artistMember.getStageName(),
                artistMember.getProfileImageUrl(),
                post
        );
    }
}
