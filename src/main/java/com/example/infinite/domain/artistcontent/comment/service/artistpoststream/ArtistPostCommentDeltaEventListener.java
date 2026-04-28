package com.example.infinite.domain.artistcontent.comment.service.artistpoststream;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ArtistPostCommentDeltaEventListener {

    private final ArtistPostCommentDeltaBuffer artistPostCommentDeltaBuffer;

    /**
     * 댓글 원본 트랜잭션과 count 집계를 분리하는 연결 지점이다.
     *
     * 댓글 DB write 가 커밋된 뒤에만 Redis delta 누적을 허용해
     * 롤백된 댓글이 count 에 섞이지 않게 한다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ArtistPostCommentDeltaEvent event) {
        // 댓글 row 트랜잭션이 실제로 커밋된 뒤에만 commentCount delta를 누적한다.
        artistPostCommentDeltaBuffer.accumulate(event.artistPostId(), event.delta());
    }
}
