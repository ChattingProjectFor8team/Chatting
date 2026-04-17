package com.example.infinite.domain.member.artist.controller;

import com.example.infinite.domain.member.artist.dto.response.ArtistPopularSearchResponse;
import com.example.infinite.domain.member.artist.dto.response.ArtistSearchResponse;
import com.example.infinite.domain.member.artist.service.ArtistSearchKeywordService;
import com.example.infinite.domain.member.artist.service.ArtistService;
import com.example.infinite.global.auth.MemberDetailsImpl;
import com.example.infinite.global.common.dto.ApiResponse;
import com.example.infinite.global.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/api/member")
@RestController
@RequiredArgsConstructor
public class ArtistController {

    private final ArtistSearchKeywordService artistSearchKeywordService;
    private final ArtistService artistService;

    @GetMapping("v1/artists/search")
    public ResponseEntity<PageResponse<ArtistSearchResponse>> searchArtistsV1(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @RequestParam(required = false) String keyword
    ) {
        // 동일 사용자의 반복 검색은 TTL 동안 한 번만 집계한다.
        artistSearchKeywordService.recordSearchKeyword(memberDetails.getEmail(), keyword);
        return ResponseEntity.ok(artistService.searchArtistsV1(keyword));
    }

    @GetMapping("v2/artists/search")
    public ResponseEntity<PageResponse<ArtistSearchResponse>> searchArtistsV2(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @RequestParam(required = false) String keyword
    ) {
        // 검색 결과 캐시 hit 여부와 무관하게 사용자 기준 인기검색어는 집계한다.
        artistSearchKeywordService.recordSearchKeyword(memberDetails.getEmail(), keyword);
        return ResponseEntity.ok(artistService.searchArtistsV2(keyword));
    }

    @GetMapping("v1/artists/search/popular")
    public ResponseEntity<ApiResponse<List<ArtistPopularSearchResponse>>> getPopularArtistSearchKeywords(
            @RequestParam(defaultValue = "10") Integer limit
    ) {
        return ResponseEntity.ok(ApiResponse.success(artistSearchKeywordService.getPopularKeywords(limit)));
    }
}
