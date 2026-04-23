package com.example.infinite.domain.member.artist.dto.response;

import com.example.infinite.domain.artistcontent.post.artistpost.dto.response.ArtistPostResponse;
import com.example.infinite.domain.artistcontent.post.fanletter.dto.response.FanLetterHotResponse;
import com.example.infinite.domain.artistcontent.post.fanpost.dto.response.FanPostResponse;

import java.util.List;

// 아티스트 홈 하이라이트는 "여러 탭 API를 클라이언트가 따로 3번 부르지 않게"
// 최신 ArtistPost 1건과 HOT 콘텐츠 묶음을 한 응답으로 조립한다.
public record ArtistHomeDashboardResponse(
        ArtistPostResponse latestArtistPost,
        List<FanPostResponse> hotFanPosts,
        List<FanLetterHotResponse> hotFanLetters
) {
}
