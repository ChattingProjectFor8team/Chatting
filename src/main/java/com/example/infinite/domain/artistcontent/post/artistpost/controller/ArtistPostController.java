package com.example.infinite.domain.artistcontent.post.artistpost.controller;

import com.example.infinite.domain.artistcontent.post.artistpost.dto.request.ArtistPostCreateRequest;
import com.example.infinite.domain.artistcontent.post.artistpost.dto.request.ArtistPostUpdateRequest;
import com.example.infinite.domain.artistcontent.post.artistpost.dto.response.ArtistPostCreateResponse;
import com.example.infinite.domain.artistcontent.post.artistpost.dto.response.ArtistPostDetailResponse;
import com.example.infinite.domain.artistcontent.post.artistpost.dto.response.ArtistPostResponse;
import com.example.infinite.domain.artistcontent.post.artistpost.service.ArtistPostService;
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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/post")
@RequiredArgsConstructor
// ArtistPost도 FanPost처럼 본문과 첨부를 한 번에 처리하는 multipart endpoint다.
// 차이는 작성 권한이 공식 아티스트 멤버로 제한된다는 점이다.
public class ArtistPostController {

    private final ArtistPostService artistPostService;

    @PostMapping(
            value = "/v1/artists/{artistId}/artist-posts",
            consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<ArtistPostCreateResponse>> createArtistPost(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @PathVariable Long artistId,
            @Valid @ModelAttribute ArtistPostCreateRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(artistPostService.create(memberDetails, artistId, request)));
    }

    @GetMapping("/v1/artists/{artistId}/artist-posts")
    public ResponseEntity<ApiResponse<CursorSliceResponse<ArtistPostResponse>>> getArtistPosts(
            @PathVariable Long artistId,
            @RequestParam(required = false) Long cursor
    ) {
        // 목록은 무한 스크롤 기준 cursor slice 응답으로 고정한다.
        return ResponseEntity.ok(ApiResponse.success(artistPostService.getArtistPosts(artistId, cursor)));
    }

    @GetMapping("/v1/artists/{artistId}/artist-posts/{artistPostId}")
    public ResponseEntity<ApiResponse<ArtistPostDetailResponse>> getArtistPost(
            @PathVariable Long artistId,
            @PathVariable Long artistPostId,
            @RequestParam(name = "commentCursor", required = false) Long commentCursor
    ) {
        // 상세는 게시글 본문 + 루트 댓글 slice를 함께 내려준다.
        return ResponseEntity.ok(ApiResponse.success(
                artistPostService.getArtistPost(artistId, artistPostId, commentCursor)
        ));
    }

    @PatchMapping(
            value = "/v1/artists/{artistId}/artist-posts/{artistPostId}",
            consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<ArtistPostResponse>> updateArtistPost(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @PathVariable Long artistId,
            @PathVariable Long artistPostId,
            @Valid @ModelAttribute ArtistPostUpdateRequest request
    ) {
        // 수정도 multipart로 받아 본문과 첨부 교체를 한 요청에서 끝낸다.
        return ResponseEntity.ok(ApiResponse.success(
                artistPostService.update(memberDetails, artistId, artistPostId, request)
        ));
    }

    @DeleteMapping("/v1/artists/{artistId}/artist-posts/{artistPostId}")
    public ResponseEntity<ApiResponse<Void>> deleteArtistPost(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @PathVariable Long artistId,
            @PathVariable Long artistPostId
    ) {
        // 삭제는 soft delete + media 정리를 서비스 계층에서 수행한다.
        artistPostService.delete(memberDetails, artistId, artistPostId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
