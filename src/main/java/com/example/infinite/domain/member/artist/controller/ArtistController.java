package com.example.infinite.domain.member.artist.controller;

import com.example.infinite.domain.member.artist.dto.response.ArtistPopularSearchResponse;
import com.example.infinite.domain.member.artist.dto.response.ArtistSearchResponse;
import com.example.infinite.domain.member.artist.service.ArtistSearchKeywordService;
import com.example.infinite.domain.member.artist.service.ArtistService;
import com.example.infinite.global.common.dto.ApiResponse;
import com.example.infinite.global.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
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
    public ApiResponse<PageResponse<ArtistSearchResponse>> searchArtistsV1(
            @RequestParam(required = false) String keyword
    ) {
        // 검색 결과 캐시 hit 여부와 무관하게 호출 수는 집계한다.
        artistSearchKeywordService.recordSearchKeyword(keyword);
        return ApiResponse.success(artistService.searchArtistsV1(keyword));
    }

    @GetMapping("v2/artists/search")
    public ApiResponse<PageResponse<ArtistSearchResponse>> searchArtistsV2(
            @RequestParam(required = false) String keyword
    ) {
        // 검색 결과 캐시 hit 여부와 무관하게 호출 수는 집계한다.
        artistSearchKeywordService.recordSearchKeyword(keyword);
        return ApiResponse.success(artistService.searchArtistsV2(keyword));
    }

    @GetMapping("v1/artists/search/popular")
    public ApiResponse<List<ArtistPopularSearchResponse>> getPopularArtistSearchKeywords(
            @RequestParam(defaultValue = "10") Integer limit
    ) {
        return ApiResponse.success(artistSearchKeywordService.getPopularKeywords(limit));
    }
}
