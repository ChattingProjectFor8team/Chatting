package com.example.infinite.domain.artistcontent.post.fanpost.dto.response;

import com.example.infinite.domain.artistcontent.comment.dto.response.CommentResponse;
import com.example.infinite.global.common.dto.CursorSliceResponse;

import java.time.LocalDateTime;
import java.util.List;

public record FanPostDetailResponse(
        Long fanPostId,
        Long artistId,
        Long writerId,
        String writerNickname,
        String writerProfileImageUrl,
        boolean fanMembershipSubscribed,
        boolean dmSubscribed,
        String content,
        long likeCount,
        long commentCount,
        int mediaCount,
        List<FanPostMediaResponse> media,
        List<String> hashtags,
        CursorSliceResponse<CommentResponse> comments,
        LocalDateTime createdAt
) {
    public static FanPostDetailResponse from(FanPostResponse fanPostResponse, CursorSliceResponse<CommentResponse> comments) {
        // 상세 응답은 기존 팬포스트 본문 구조를 유지한 채 댓글 슬라이스만 추가로 결합한다.
        return new FanPostDetailResponse(
                fanPostResponse.fanPostId(),
                fanPostResponse.artistId(),
                fanPostResponse.writerId(),
                fanPostResponse.writerNickname(),
                fanPostResponse.writerProfileImageUrl(),
                fanPostResponse.fanMembershipSubscribed(),
                fanPostResponse.dmSubscribed(),
                fanPostResponse.content(),
                fanPostResponse.likeCount(),
                fanPostResponse.commentCount(),
                fanPostResponse.mediaCount(),
                fanPostResponse.media(),
                fanPostResponse.hashtags(),
                comments,
                fanPostResponse.createdAt()
        );
    }
}
