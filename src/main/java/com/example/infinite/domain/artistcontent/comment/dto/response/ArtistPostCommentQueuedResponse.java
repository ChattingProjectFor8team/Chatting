package com.example.infinite.domain.artistcontent.comment.dto.response;

import java.time.LocalDateTime;

public record ArtistPostCommentQueuedResponse(
        String requestId,
        String commandType,
        Long artistPostId,
        Long parentCommentId,
        Long commentId,
        LocalDateTime queuedAt
) {
    // v2 댓글 API는 worker 처리 전이라 CommentResponse를 바로 줄 수 없으므로 queued 메타정보만 반환한다.
}
