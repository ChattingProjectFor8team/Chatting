package com.example.infinite.domain.artistcontent.follow.controller;

import com.example.infinite.domain.artistcontent.follow.dto.response.FollowResponse;
import com.example.infinite.domain.artistcontent.follow.service.FollowService;
import com.example.infinite.global.auth.MemberDetailsImpl;
import com.example.infinite.global.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member/v1/follows")
public class FollowController {

    private final FollowService followService;

    @PostMapping("/artist-members/{artistMemberId}/toggle")
    public ResponseEntity<ApiResponse<FollowResponse>> toggleArtistMemberFollow(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @PathVariable Long artistMemberId
    ) {
        // 요청 body 없이 pathVariable 만으로 토글하면
        // 프론트가 "카드/프로필의 follow 버튼" 어디서든 같은 API를 재사용하기 쉽다.
        return ResponseEntity.ok(ApiResponse.success(
                followService.toggleArtistMemberFollow(memberDetails, artistMemberId)
        ));
    }
}
