package com.example.infinite.domain.member.home.controller;

import com.example.infinite.domain.member.home.dto.response.MemberHomeDashboardResponse;
import com.example.infinite.domain.member.home.service.MemberHomeDashboardService;
import com.example.infinite.global.auth.MemberDetailsImpl;
import com.example.infinite.global.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member/v1/home")
public class MemberHomeDashboardController {

    private final MemberHomeDashboardService memberHomeDashboardService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<MemberHomeDashboardResponse>> getDashboard(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails
    ) {
        // 메인 홈은 이미 존재하는 검색 API를 대체하지 않고,
        // "인기 검색어 + 구독 아티스트 최신 글 + 팔로우 멤버 최신 글"만 한 번에 모아준다.
        return ResponseEntity.ok(ApiResponse.success(
                memberHomeDashboardService.getDashboard(memberDetails)
        ));
    }
}
