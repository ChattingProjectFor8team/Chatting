package com.example.infinite.domain.artistcontent.post.fanpost.controller;

import com.example.infinite.domain.artistcontent.post.fanpost.dto.request.FanPostCreateRequest;
import com.example.infinite.domain.artistcontent.post.fanpost.dto.request.FanPostUpdateRequest;
import com.example.infinite.domain.artistcontent.post.fanpost.dto.response.FanPostCreateResponse;
import com.example.infinite.domain.artistcontent.post.fanpost.dto.response.FanPostResponse;
import com.example.infinite.domain.artistcontent.post.fanpost.service.FanPostService;
import com.example.infinite.global.auth.MemberDetailsImpl;
import com.example.infinite.global.common.dto.ApiResponse;
import com.example.infinite.global.common.dto.CursorSliceResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/post")
@RequiredArgsConstructor
public class FanPostController {

    private final FanPostService fanPostService;

    @PostMapping("/v1/artists/{artistId}/fan-posts")
    public ResponseEntity<ApiResponse<FanPostCreateResponse>> createFanPost(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @PathVariable Long artistId,
            @Valid @RequestBody FanPostCreateRequest request
    ) {
        // 팬 게시글은 특정 아티스트 커뮤니티에 종속되므로 artistId를 path variable로 고정한다.
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(fanPostService.create(memberDetails, artistId, request)));
    }

    @GetMapping("/v1/artists/{artistId}/fan-posts")
    public ResponseEntity<ApiResponse<CursorSliceResponse<FanPostResponse>>> getFanPosts(
            @PathVariable Long artistId,
            @RequestParam(required = false) Long cursor
    ) {
        // 전체 개수 기반 page보다 무한 스크롤 UX가 중요하므로 cursor slice 응답을 사용한다.
        return ResponseEntity.ok(ApiResponse.success(fanPostService.getFanPosts(artistId, cursor)));
    }

    @GetMapping("/v1/artists/{artistId}/fan-posts/{fanPostId}")
    public ResponseEntity<ApiResponse<FanPostResponse>> getFanPost(
            @PathVariable Long artistId,
            @PathVariable Long fanPostId
    ) {
        // 상세도 artist 하위 경로를 유지해 잘못된 소속 커뮤니티 접근을 URL 단계에서 드러낸다.
        return ResponseEntity.ok(ApiResponse.success(fanPostService.getFanPost(artistId, fanPostId)));
    }

    @PatchMapping("/v1/artists/{artistId}/fan-posts/{fanPostId}")
    public ResponseEntity<ApiResponse<FanPostResponse>> updateFanPost(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @PathVariable Long artistId,
            @PathVariable Long fanPostId,
            @Valid @RequestBody FanPostUpdateRequest request
    ) {
        // 수정 권한은 서비스에서 작성자 본인 여부까지 확인한다.
        return ResponseEntity.ok(ApiResponse.success(
                fanPostService.update(memberDetails, artistId, fanPostId, request)
        ));
    }

    @DeleteMapping("/v1/artists/{artistId}/fan-posts/{fanPostId}")
    public ResponseEntity<ApiResponse<Void>> deleteFanPost(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @PathVariable Long artistId,
            @PathVariable Long fanPostId
    ) {
        // 삭제는 hard delete 대신 soft delete 전환으로 처리한다.
        fanPostService.delete(memberDetails, artistId, fanPostId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
