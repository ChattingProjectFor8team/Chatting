package com.example.infinite.domain.dm.controller;

import com.example.infinite.domain.dm.dto.response.DmMessageResponse;
import com.example.infinite.domain.dm.dto.response.DmRoomResponse;
import com.example.infinite.domain.dm.service.DmService;
import com.example.infinite.global.auth.MemberDetailsImpl;
import com.example.infinite.global.common.dto.ApiResponse;
import com.example.infinite.global.common.dto.CursorSliceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class DmController {

    private final DmService dmService;

    @GetMapping("/api/v1/dm/rooms")
    public ResponseEntity<ApiResponse<List<DmRoomResponse>>> getMyRooms(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                dmService.getMyRooms(memberDetails.getMemberId())));
    }

    @GetMapping("/api/v1/dm/rooms/{roomId}/messages")
    public ResponseEntity<ApiResponse<CursorSliceResponse<DmMessageResponse>>> getRoomMessages(
            @PathVariable Long roomId,
            @RequestParam(required = false) Long before,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal MemberDetailsImpl memberDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                dmService.getRoomMessages(roomId, memberDetails.getMemberId(), before, size)));
    }
}
