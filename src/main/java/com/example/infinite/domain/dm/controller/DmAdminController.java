package com.example.infinite.domain.dm.controller;

import com.example.infinite.domain.dm.dto.response.DmRoomResponse;
import com.example.infinite.domain.dm.service.DmService;
import com.example.infinite.global.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/artists/{artistId}/dm")
@RequiredArgsConstructor
public class DmAdminController {

    private final DmService dmService;

    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<List<DmRoomResponse>>> getArtistRooms(
            @PathVariable Long artistId) {
        return ResponseEntity.ok(ApiResponse.success(dmService.getArtistRooms(artistId)));
    }
}
