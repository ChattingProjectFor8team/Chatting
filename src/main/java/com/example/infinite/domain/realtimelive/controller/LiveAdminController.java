package com.example.infinite.domain.realtimelive.controller;

import com.example.infinite.domain.realtimelive.dto.request.LiveCreateRequest;
import com.example.infinite.domain.realtimelive.dto.response.LiveResponse;
import com.example.infinite.domain.realtimelive.service.RealtimeLiveService;
import com.example.infinite.global.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
            @PathVariable Long artistId,
            @Valid @RequestBody LiveCreateRequest request) {
        LiveResponse response = realtimeLiveService.createLive(artistId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PatchMapping("/{liveId}/start")
    public ResponseEntity<ApiResponse<LiveResponse>> startLive(
            @PathVariable Long artistId,
            @PathVariable Long liveId) {
        return ResponseEntity.ok(ApiResponse.success(realtimeLiveService.startLive(artistId, liveId)));
    }

    @PatchMapping("/{liveId}/end")
    public ResponseEntity<ApiResponse<LiveResponse>> endLive(
            @PathVariable Long artistId,
            @PathVariable Long liveId) {
        return ResponseEntity.ok(ApiResponse.success(realtimeLiveService.endLive(artistId, liveId)));
    }

    @DeleteMapping("/{liveId}/chat/messages/{messageId}")
    public ResponseEntity<ApiResponse<Void>> deleteChatMessage(
            @PathVariable Long artistId,
            @PathVariable Long liveId,
            @PathVariable Long messageId) {
        realtimeLiveService.deleteChatMessage(artistId, liveId, messageId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
