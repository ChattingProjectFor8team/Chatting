package com.example.infinite.domain.artistcontent.hashtag.controller;

import com.example.infinite.domain.artistcontent.hashtag.dto.response.HashtagResponse;
import com.example.infinite.domain.artistcontent.hashtag.service.HashtagService;
import com.example.infinite.global.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 해시태그 추천 검색 API를 노출한다.
 */
@RestController
@RequestMapping("/api/post")
@RequiredArgsConstructor
public class HashtagController {

    private final HashtagService hashtagService;

    @GetMapping("/v1/hashtags/suggestions")
    public ResponseEntity<ApiResponse<List<HashtagResponse>>> getHashtagSuggestions(
            @RequestParam(name = "keyword") String keyword,
            @RequestParam(name = "limit",  defaultValue = "5") Integer limit
    ) {
        // 전역 해시태그 추천은 이미 유지 중인 usageCount 비정규화 컬럼을 그대로 활용한다.
        return ResponseEntity.ok(ApiResponse.success(hashtagService.getSuggestions(keyword, limit)));
    }
}
