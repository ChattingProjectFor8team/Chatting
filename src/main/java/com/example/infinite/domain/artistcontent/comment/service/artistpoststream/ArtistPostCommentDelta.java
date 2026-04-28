package com.example.infinite.domain.artistcontent.comment.service.artistpoststream;

/**
 * flush scheduler 가 DB에 반영할 commentCount 증감치만 담는 값 객체다.
 *
 * 핵심은 "댓글 row 전체"가 아니라
 * "어느 게시글에 몇 개를 더하고 뺄지"만 들고 다니는 것이다.
 */
public record ArtistPostCommentDelta(
        Long artistPostId,
        long delta
) {
}
