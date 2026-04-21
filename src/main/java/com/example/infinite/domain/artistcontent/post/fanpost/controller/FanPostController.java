package com.example.infinite.domain.artistcontent.post.fanpost.controller;

import com.example.infinite.domain.artistcontent.post.fanpost.dto.request.FanPostCreateRequest;
import com.example.infinite.domain.artistcontent.post.fanpost.dto.request.FanPostUpdateRequest;
import com.example.infinite.domain.artistcontent.post.fanpost.dto.response.FanPostCreateResponse;
import com.example.infinite.domain.artistcontent.post.fanpost.dto.response.FanPostDetailResponse;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ModelAttribute;

@RestController
@RequestMapping("/api/post")
@RequiredArgsConstructor
// 팬포스트는 "본문 JSON"과 "첨부파일 업로드 API"를 분리하지 않고, 한 요청에서 같이 처리한다.
// 그래서 create/update 둘 다 multipart/form-data 로 받고 서비스에서 글 저장과 media 연결을 함께 끝낸다.
public class FanPostController {

    private final FanPostService fanPostService;

    @PostMapping(
            value = "/v1/artists/{artistId}/fan-posts",
            consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<FanPostCreateResponse>> createFanPost(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @PathVariable Long artistId,
            @Valid @ModelAttribute FanPostCreateRequest request
    ) {
        // 팬 게시글 작성은 본문과 첨부파일을 한 번에 처리하므로 multipart/form-data 로 받는다.
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
    public ResponseEntity<ApiResponse<FanPostDetailResponse>> getFanPost(
            @PathVariable Long artistId,
            @PathVariable Long fanPostId,
            @RequestParam(name = "commentCursor", required = false) Long commentCursor
    ) {
        // 상세도 artist 하위 경로를 유지해 잘못된 소속 커뮤니티 접근을 URL 단계에서 드러낸다.
        return ResponseEntity.ok(ApiResponse.success(fanPostService.getFanPost(artistId, fanPostId, commentCursor)));
    }

    @PatchMapping(
            value = "/v1/artists/{artistId}/fan-posts/{fanPostId}",
            consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<FanPostResponse>> updateFanPost(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @PathVariable Long artistId,
            @PathVariable Long fanPostId,
            @Valid @ModelAttribute FanPostUpdateRequest request
    ) {
        // 수정도 multipart 로 받아 본문과 미디어 교체를 한 요청에서 처리한다.
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
