package com.example.infinite.domain.artistcontent.comment.service.cache;

import com.example.infinite.domain.artistcontent.post.enums.PostType;

/**
 * 댓글 생성/삭제 후 어떤 댓글 캐시 범위를 비워야 하는지 전달하는 이벤트다.
 *
 * root comment slice는 post 단위로,
 * replies는 root thread 단위로 관리하므로
 * invalidation도 그 두 축으로 분리해 들고 간다.
 */
public record CommentCacheInvalidationEvent(
        PostType targetType,
        Long targetId,
        Long rootCommentId
) {
    public static CommentCacheInvalidationEvent forRootSlice(PostType targetType, Long targetId) {
        return new CommentCacheInvalidationEvent(targetType, targetId, null);
    }

    public static CommentCacheInvalidationEvent forThread(PostType targetType, Long targetId, Long rootCommentId) {
        return new CommentCacheInvalidationEvent(targetType, targetId, rootCommentId);
    }
}
