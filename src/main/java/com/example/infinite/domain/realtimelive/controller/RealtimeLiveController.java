package com.example.infinite.domain.realtimelive.controller;

import com.example.infinite.domain.realtimelive.dto.response.LiveChatMessageResponse;
import com.example.infinite.domain.realtimelive.dto.response.LiveResponse;
import com.example.infinite.domain.realtimelive.dto.response.LiveVodResponse;
import com.example.infinite.domain.realtimelive.enums.LiveStatus;
import com.example.infinite.domain.realtimelive.service.RealtimeLiveService;
import com.example.infinite.global.common.dto.ApiResponse;
import com.example.infinite.global.common.dto.CursorSliceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RealtimeLiveController {

    private final RealtimeLiveService realtimeLiveService;

    @GetMapping("/api/v1/artists/{artistId}/lives")
    public ResponseEntity<ApiResponse<List<LiveResponse>>> getLiveList(
            @PathVariable Long artistId,
            @RequestParam(required = false) LiveStatus status) {
        return ResponseEntity.ok(ApiResponse.success(realtimeLiveService.getLiveList(artistId, status)));
    }

    @GetMapping("/api/v1/artists/{artistId}/lives/{liveId}")
    public ResponseEntity<ApiResponse<LiveResponse>> getLiveDetail(
            @PathVariable Long artistId,
            @PathVariable Long liveId) {
        return ResponseEntity.ok(ApiResponse.success(realtimeLiveService.getLiveDetail(artistId, liveId)));
    }

    @GetMapping("/api/v1/artists/{artistId}/lives/vods")
    public ResponseEntity<ApiResponse<CursorSliceResponse<LiveVodResponse>>> getVodList(
            @PathVariable Long artistId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer size
    ) {
        // 라이브 탭의 종료 방송 목록은 REPLAY_READY 상태만 id DESC 커서 슬라이스로 내려준다.
        return ResponseEntity.ok(ApiResponse.success(
                realtimeLiveService.getVodList(artistId, cursor, size)
        ));
    }

    @GetMapping("/api/v1/artists/{artistId}/lives/{liveId}/chat/messages")
    public ResponseEntity<ApiResponse<CursorSliceResponse<LiveChatMessageResponse>>> getChatMessages(
            @PathVariable Long artistId,
            @PathVariable Long liveId,
            @RequestParam(required = false) Long before,
            @RequestParam(defaultValue = "50") int size) {
        // 채팅 내역은 기존 live detail 하위 경로를 유지하고, before=id 기반 커서를 그대로 재사용한다.
        return ResponseEntity.ok(ApiResponse.success(
                realtimeLiveService.getChatMessages(liveId, before, size)));
    }
}
