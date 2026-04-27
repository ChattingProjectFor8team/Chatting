package com.example.infinite.domain.artistcontent.comment.controller;

import com.example.infinite.domain.artistcontent.comment.dto.request.CommentCreateRequest;
import com.example.infinite.domain.artistcontent.comment.dto.response.ArtistPostCommentQueuedResponse;
import com.example.infinite.domain.artistcontent.comment.dto.response.CommentResponse;
import com.example.infinite.domain.artistcontent.comment.service.CommentService;
import com.example.infinite.global.auth.MemberDetailsImpl;
import com.example.infinite.global.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.infinite.global.common.dto.CursorSliceResponse;

@RestController
@RequestMapping("/api/post")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/v1/artists/{artistId}/fan-posts/{fanPostId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> createFanPostComment(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @PathVariable Long artistId,
            @PathVariable Long fanPostId,
            @Valid @RequestBody CommentCreateRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        commentService.createFanPostComment(memberDetails, artistId, fanPostId, request)
                ));
    }

    @PostMapping("/v1/artists/{artistId}/artist-posts/{artistPostId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> createArtistPostComment(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @PathVariable Long artistId,
            @PathVariable Long artistPostId,
            @Valid @RequestBody CommentCreateRequest request
    ) {
        // legacy compatibility route.
        // ArtistPost 댓글의 실제 최신 사용 경로는 v2이며, v1은 형태 보존용으로만 남겨둔다.
        // 새 연동/수정은 이 경로 기준으로 진행하지 않는다.
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        commentService.createArtistPostComment(memberDetails, artistId, artistPostId, request)
                ));
    }

    @PostMapping("/v2/artists/{artistId}/artist-posts/{artistPostId}/comments")
    public ResponseEntity<ApiResponse<ArtistPostCommentQueuedResponse>> createArtistPostCommentV2(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @PathVariable Long artistId,
            @PathVariable Long artistPostId,
            @Valid @RequestBody CommentCreateRequest request
    ) {
        // v2는 댓글 원본 저장을 worker가 처리하므로, API는 "접수됨"만 응답한다.
        // ArtistPost 댓글의 실제 사용 경로는 이 v2다.
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(
                        commentService.queueArtistPostCommentV2(memberDetails, artistId, artistPostId, request)
                ));
    }

    @GetMapping("/v1/artists/{artistId}/fan-posts/{fanPostId}/comments/{commentId}/replies")
    public ResponseEntity<ApiResponse<CursorSliceResponse<CommentResponse>>> getFanPostReplies(
            @PathVariable Long artistId,
            @PathVariable Long fanPostId,
            @PathVariable Long commentId,
            @RequestParam(required = false) Long cursor
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                commentService.getFanPostReplies(artistId, fanPostId, commentId, cursor)
        ));
    }

    @GetMapping("/v1/artists/{artistId}/artist-posts/{artistPostId}/comments/{commentId}/replies")
    public ResponseEntity<ApiResponse<CursorSliceResponse<CommentResponse>>> getArtistPostReplies(
            @PathVariable Long artistId,
            @PathVariable Long artistPostId,
            @PathVariable Long commentId,
            @RequestParam(required = false) Long cursor
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                commentService.getArtistPostReplies(artistId, artistPostId, commentId, cursor)
        ));
    }

    @DeleteMapping("/v1/artists/{artistId}/fan-posts/{fanPostId}/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteFanPostComment(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @PathVariable Long artistId,
            @PathVariable Long fanPostId,
            @PathVariable Long commentId
    ) {
        commentService.deleteFanPostComment(memberDetails, artistId, fanPostId, commentId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/v1/artists/{artistId}/artist-posts/{artistPostId}/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteArtistPostComment(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @PathVariable Long artistId,
            @PathVariable Long artistPostId,
            @PathVariable Long commentId
    ) {
        // legacy compatibility route.
        // ArtistPost 댓글 삭제의 실제 최신 사용 경로는 v2이며, v1 delete도 형태만 유지한다.
        commentService.deleteArtistPostComment(memberDetails, artistId, artistPostId, commentId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/v2/artists/{artistId}/artist-posts/{artistPostId}/comments/{commentId}")
    public ResponseEntity<ApiResponse<ArtistPostCommentQueuedResponse>> deleteArtistPostCommentV2(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @PathVariable Long artistId,
            @PathVariable Long artistPostId,
            @PathVariable Long commentId
    ) {
        // ArtistPost 댓글 삭제의 실제 사용 경로는 이 v2다.
        // 실제 delete/placeholder 전환은 consumer가 thread lock 안에서 수행한다.
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(
                        commentService.queueDeleteArtistPostCommentV2(memberDetails, artistId, artistPostId, commentId)
                ));
    }
}
