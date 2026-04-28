package com.example.infinite.domain.artistcontent.media.controller;

import com.example.infinite.domain.artistcontent.media.dto.request.ArtistYoutubeVideoImportRequest;
import com.example.infinite.domain.artistcontent.media.dto.response.ArtistYoutubeVideoResponse;
import com.example.infinite.domain.artistcontent.media.service.ArtistYoutubeVideoService;
import com.example.infinite.global.auth.MemberDetailsImpl;
import com.example.infinite.global.common.dto.ApiResponse;
import com.example.infinite.global.common.dto.CursorSliceResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/media/v1")
@RequiredArgsConstructor
public class ArtistYoutubeVideoController {

    private final ArtistYoutubeVideoService artistYoutubeVideoService;

    @PostMapping("/artists/{artistId}/youtube-videos")
    public ResponseEntity<ApiResponse<ArtistYoutubeVideoResponse>> importYoutubeVideo(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @PathVariable Long artistId,
            @Valid @RequestBody ArtistYoutubeVideoImportRequest request
    ) {
        // 유튜브 링크 1개를 메타데이터 카드로 저장하는 import API 다.
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                artistYoutubeVideoService.importYoutubeVideo(memberDetails, artistId, request)
        ));
    }

    @GetMapping("/artists/{artistId}/youtube-videos")
    public ResponseEntity<ApiResponse<CursorSliceResponse<ArtistYoutubeVideoResponse>>> getYoutubeVideos(
            @PathVariable Long artistId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer size
    ) {
        // 미디어 탭은 무한 스크롤이 자연스러워 전체 page 대신 id DESC 커서 슬라이스를 사용한다.
        return ResponseEntity.ok(ApiResponse.success(
                artistYoutubeVideoService.getYoutubeVideos(artistId, cursor, size)
        ));
    }
}
