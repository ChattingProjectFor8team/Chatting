package com.example.infinite.domain.member.home.dto.response;

import com.example.infinite.domain.artistcontent.post.artistpost.dto.response.ArtistPostResponse;

import java.util.List;

// 메인 홈에서 "구독한 아티스트 카드 1개 + 최신 공식글 묶음"을 그대로 표현하는 응답이다.
public record SubscribedArtistLatestPostsResponse(
        HomeDashboardArtistSummaryResponse artist,
        List<ArtistPostResponse> posts
) {
}
