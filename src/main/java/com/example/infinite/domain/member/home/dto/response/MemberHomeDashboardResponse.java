package com.example.infinite.domain.member.home.dto.response;

import com.example.infinite.domain.member.artist.dto.response.ArtistPopularSearchResponse;

import java.util.List;

// 메인 홈은 검색 자체를 대체하지 않고, 홈 진입 직후 필요한 요약 섹션만 묶는다.
public record MemberHomeDashboardResponse(
        List<ArtistPopularSearchResponse> popularKeywords,
        List<SubscribedArtistLatestPostsResponse> subscribedArtistsLatestPosts,
        List<FollowedArtistMemberLatestPostResponse> followedArtistMembersLatestPosts
) {
}
