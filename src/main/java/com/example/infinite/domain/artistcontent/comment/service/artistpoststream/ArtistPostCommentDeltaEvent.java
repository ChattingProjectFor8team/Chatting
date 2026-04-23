package com.example.infinite.domain.artistcontent.comment.service.artistpoststream;

/**
 * 댓글 트랜잭션이 끝난 뒤 Redis delta 누적으로 넘기기 위한 경량 이벤트다.
 *
 * DB write 와 count 집계를 분리하되,
 * 이벤트 자체는 "게시글 id + delta" 만큼만 남겨 비용을 줄인다.
 */
public record ArtistPostCommentDeltaEvent(
        Long artistPostId,
        long delta
) {
}
