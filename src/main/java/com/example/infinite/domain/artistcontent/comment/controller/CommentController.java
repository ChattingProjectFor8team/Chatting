package com.example.infinite.domain.artistcontent.comment.controller;

import com.example.infinite.domain.artistcontent.comment.dto.request.CommentCreateRequest;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
        // 아티스트 게시글도 댓글 정책은 같고, URL만 게시글 종류에 맞춰 분리한다.
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        commentService.createArtistPostComment(memberDetails, artistId, artistPostId, request)
                ));
    }

    @GetMapping("/v1/artists/{artistId}/fan-posts/{fanPostId}/comments/{commentId}/replies")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getFanPostReplies(
            @PathVariable Long artistId,
            @PathVariable Long fanPostId,
            @PathVariable Long commentId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                commentService.getFanPostReplies(artistId, fanPostId, commentId)
        ));
    }

    @GetMapping("/v1/artists/{artistId}/artist-posts/{artistPostId}/comments/{commentId}/replies")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getArtistPostReplies(
            @PathVariable Long artistId,
            @PathVariable Long artistPostId,
            @PathVariable Long commentId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                commentService.getArtistPostReplies(artistId, artistPostId, commentId)
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
        // 삭제 역시 artist-post 경로를 별도로 두되 서비스 공통 로직으로 위임한다.
        commentService.deleteArtistPostComment(memberDetails, artistId, artistPostId, commentId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
