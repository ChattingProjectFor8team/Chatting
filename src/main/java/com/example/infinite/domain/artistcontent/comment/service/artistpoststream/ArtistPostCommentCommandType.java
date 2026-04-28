package com.example.infinite.domain.artistcontent.comment.service.artistpoststream;

/**
 * 댓글 stream consumer 가 어떤 작업을 수행해야 하는지 나타내는 명령 타입이다.
 *
 * 댓글은 "생성/삭제" 모두 같은 stream 으로 흘리므로
 * consumer 는 이 enum 으로 분기해 실제 코어 로직을 호출한다.
 */
public enum ArtistPostCommentCommandType {
    // parentCommentId 가 없는 원댓글 생성
    CREATE_ROOT,
    // parentCommentId 가 있는 대댓글 생성
    CREATE_REPLY,
    // commentId 기준 삭제 또는 placeholder 전환
    DELETE
}
