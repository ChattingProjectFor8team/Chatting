package com.example.infinite.domain.artistcontent.post.artistpost.dto.response;

import com.example.infinite.domain.artistcontent.comment.dto.response.CommentResponse;
import com.example.infinite.global.common.dto.CursorSliceResponse;

import java.time.LocalDateTime;
import java.util.List;

// 상세 응답은 목록 구조를 유지한 채 댓글 슬라이스만 추가한 형태다.
public record ArtistPostDetailResponse(
        Long artistPostId,
        Long artistId,
        Long writerId,
        String writerNickname,
        String writerProfileImageUrl,
        boolean artistBadge,
        String content,
        long likeCount,
        long commentCount,
        int mediaCount,
        List<ArtistPostMediaResponse> media,
        List<String> hashtags,
        CursorSliceResponse<CommentResponse> comments,
        LocalDateTime createdAt
) {
    public static ArtistPostDetailResponse from(
            ArtistPostResponse artistPostResponse,
            CursorSliceResponse<CommentResponse> comments
    ) {
        // 상세 응답은 목록 구조를 유지한 채 댓글 슬라이스만 추가한다.
        return new ArtistPostDetailResponse(
                artistPostResponse.artistPostId(),
                artistPostResponse.artistId(),
                artistPostResponse.writerId(),
                artistPostResponse.writerNickname(),
                artistPostResponse.writerProfileImageUrl(),
                artistPostResponse.artistBadge(),
                artistPostResponse.content(),
                artistPostResponse.likeCount(),
                artistPostResponse.commentCount(),
                artistPostResponse.mediaCount(),
                artistPostResponse.media(),
                artistPostResponse.hashtags(),
                comments,
                artistPostResponse.createdAt()
        );
    }
}
