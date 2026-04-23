package com.example.infinite.domain.member.artist.service;

import com.example.infinite.domain.artistcontent.post.artistpost.dto.response.ArtistPostResponse;
import com.example.infinite.domain.artistcontent.post.artistpost.service.ArtistPostService;
import com.example.infinite.domain.artistcontent.post.fanletter.dto.response.FanLetterHotResponse;
import com.example.infinite.domain.artistcontent.post.fanletter.service.FanLetterService;
import com.example.infinite.domain.artistcontent.post.fanpost.dto.response.FanPostResponse;
import com.example.infinite.domain.artistcontent.post.fanpost.service.FanPostService;
import com.example.infinite.domain.member.artist.dto.response.ArtistHomeDashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArtistHomeDashboardService {

    private static final int DASHBOARD_HOT_FAN_POST_SIZE = 6;
    private static final int DASHBOARD_HOT_FAN_LETTER_SIZE = 4;

    private final ArtistPostService artistPostService;
    private final FanPostService fanPostService;
    private final FanLetterService fanLetterService;

    public ArtistHomeDashboardResponse getArtistHomeDashboard(Long artistId) {
        // 대시보드는 별도 집계 테이블 없이 "지금 시점의 최신 1건 + HOT 첫 묶음"을 조립한다.
        // 현재 단계에서는 캐시보다 응답 정책을 먼저 고정하는 것이 중요하다고 보고 논캐시로 둔다.
        ArtistPostResponse latestArtistPost = artistPostService.getLatestArtistPost(artistId);
        List<FanPostResponse> hotFanPosts = fanPostService
                .getHotFanPosts(artistId, null, null, DASHBOARD_HOT_FAN_POST_SIZE)
                .content();
        List<FanLetterHotResponse> hotFanLetters = fanLetterService
                .getHotFanLetters(artistId, 0, DASHBOARD_HOT_FAN_LETTER_SIZE)
                .content();

        return new ArtistHomeDashboardResponse(latestArtistPost, hotFanPosts, hotFanLetters);
    }
}
