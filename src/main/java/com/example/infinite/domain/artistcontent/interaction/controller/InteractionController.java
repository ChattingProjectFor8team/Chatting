package com.example.infinite.domain.artistcontent.interaction.controller;

import com.example.infinite.domain.artistcontent.interaction.dto.response.InteractionResponse;
import com.example.infinite.domain.artistcontent.interaction.service.InteractionService;
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
@RequestMapping("/api/post")
@RequiredArgsConstructor
public class InteractionController {

    private final InteractionService interactionService;

    @PostMapping("/v1/artists/{artistId}/fan-posts/{fanPostId}/likes/toggle")
    public ResponseEntity<ApiResponse<InteractionResponse>> toggleFanPostLike(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @PathVariable Long artistId,
            @PathVariable Long fanPostId
    ) {
        // 게시글 하위 경로를 유지해 어떤 artist 커뮤니티의 좋아요인지 URL만 보고 알 수 있게 한다.
        return ResponseEntity.ok(ApiResponse.success(
                interactionService.toggleFanPostLike(memberDetails, artistId, fanPostId)
        ));
    }

    @PostMapping("/v1/artists/{artistId}/artist-posts/{artistPostId}/likes/toggle")
    public ResponseEntity<ApiResponse<InteractionResponse>> toggleArtistPostLike(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @PathVariable Long artistId,
            @PathVariable Long artistPostId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                interactionService.toggleArtistPostLike(memberDetails, artistId, artistPostId)
        ));
    }
}
