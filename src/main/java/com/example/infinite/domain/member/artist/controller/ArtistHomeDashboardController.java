package com.example.infinite.domain.member.artist.controller;

import com.example.infinite.domain.member.artist.dto.response.ArtistHomeDashboardResponse;
import com.example.infinite.domain.member.artist.service.ArtistHomeDashboardService;
import com.example.infinite.global.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class ArtistHomeDashboardController {

    private final ArtistHomeDashboardService artistHomeDashboardService;

    @GetMapping("v1/artists/{artistId}/dashboard")
    public ResponseEntity<ApiResponse<ArtistHomeDashboardResponse>> getArtistHomeDashboard(
            @PathVariable Long artistId
    ) {
        // 아티스트 홈 하이라이트는 탭별 API를 클라이언트가 따로 호출하지 않게
        // 최신 ArtistPost 1건 + HOT FanPost/FanLetter 묶음을 한 번에 내려준다.
        return ResponseEntity.ok(ApiResponse.success(
                artistHomeDashboardService.getArtistHomeDashboard(artistId)
        ));
    }
}
