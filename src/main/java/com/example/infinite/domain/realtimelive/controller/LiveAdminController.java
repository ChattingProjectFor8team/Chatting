package com.example.infinite.domain.realtimelive.controller;

import com.example.infinite.domain.realtimelive.dto.request.LiveCreateRequest;
import com.example.infinite.domain.realtimelive.dto.request.LiveReplayPublishRequest;
import com.example.infinite.domain.realtimelive.dto.response.LiveResponse;
import com.example.infinite.domain.realtimelive.service.RealtimeLiveService;
import com.example.infinite.global.auth.MemberDetailsImpl;
import com.example.infinite.global.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/artists/{artistId}/lives")
@RequiredArgsConstructor
public class LiveAdminController {

    private final RealtimeLiveService realtimeLiveService;

    @PostMapping
    public ResponseEntity<ApiResponse<LiveResponse>> createLive(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @PathVariable Long artistId,
            @Valid @RequestBody LiveCreateRequest request) {
        // 라이브 생성자는 실제 해당 artist 소속 멤버여야 하므로 인증 주체를 서비스로 전달한다.
        LiveResponse response = realtimeLiveService.createLive(memberDetails, artistId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PatchMapping("/{liveId}/start")
    public ResponseEntity<ApiResponse<LiveResponse>> startLive(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @PathVariable Long artistId,
            @PathVariable Long liveId) {
        return ResponseEntity.ok(ApiResponse.success(realtimeLiveService.startLive(memberDetails, artistId, liveId)));
    }

    @PatchMapping("/{liveId}/end")
    public ResponseEntity<ApiResponse<LiveResponse>> endLive(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @PathVariable Long artistId,
            @PathVariable Long liveId) {
        return ResponseEntity.ok(ApiResponse.success(realtimeLiveService.endLive(memberDetails, artistId, liveId)));
    }

    @PatchMapping("/{liveId}/replay")
    public ResponseEntity<ApiResponse<LiveResponse>> publishReplay(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @PathVariable Long artistId,
            @PathVariable Long liveId,
            @Valid @RequestBody LiveReplayPublishRequest request
    ) {
        // 녹화본 URL 이 준비된 뒤 이 API 를 호출하면 public VOD 목록에 자동 노출된다.
        return ResponseEntity.ok(ApiResponse.success(
                realtimeLiveService.publishReplay(memberDetails, artistId, liveId, request)
        ));
    }

    @DeleteMapping("/{liveId}/chat/messages/{messageId}")
    public ResponseEntity<ApiResponse<Void>> deleteChatMessage(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @PathVariable Long artistId,
            @PathVariable Long liveId,
            @PathVariable Long messageId) {
        realtimeLiveService.deleteChatMessage(memberDetails, artistId, liveId, messageId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{liveId}/chat/mute/{userId}")
    public ResponseEntity<ApiResponse<Void>> muteUser(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @PathVariable Long artistId,
            @PathVariable Long liveId,
            @PathVariable Long userId) {
        realtimeLiveService.muteUser(memberDetails, artistId, liveId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{liveId}/chat/mute/{userId}")
    public ResponseEntity<ApiResponse<Void>> unmuteUser(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @PathVariable Long artistId,
            @PathVariable Long liveId,
            @PathVariable Long userId) {
        realtimeLiveService.unmuteUser(memberDetails, artistId, liveId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
