package com.example.infinite.domain.artistcontent.comment.service.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class CommentCacheInvalidationListener {

    private final CommentQueryCacheService commentQueryCacheService;

    /**
     * 댓글 캐시는 실제 DB write가 커밋된 뒤에만 비운다.
     * 그래야 롤백된 댓글 생성/삭제 때문에 정상 캐시를 불필요하게 날리지 않는다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(CommentCacheInvalidationEvent event) {
        commentQueryCacheService.evictRootSliceScope(event.targetType(), event.targetId());
        if (event.rootCommentId() != null) {
            commentQueryCacheService.evictReplyScope(event.targetType(), event.targetId(), event.rootCommentId());
        }
    }
}
