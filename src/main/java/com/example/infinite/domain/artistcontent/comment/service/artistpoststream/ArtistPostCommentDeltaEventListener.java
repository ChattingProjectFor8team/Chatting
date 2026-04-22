package com.example.infinite.domain.artistcontent.comment.service.artistpoststream;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ArtistPostCommentDeltaEventListener {

    private final ArtistPostCommentDeltaBuffer artistPostCommentDeltaBuffer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ArtistPostCommentDeltaEvent event) {
        // 댓글 row 트랜잭션이 실제로 커밋된 뒤에만 commentCount delta를 누적한다.
        artistPostCommentDeltaBuffer.accumulate(event.artistPostId(), event.delta());
    }
}
