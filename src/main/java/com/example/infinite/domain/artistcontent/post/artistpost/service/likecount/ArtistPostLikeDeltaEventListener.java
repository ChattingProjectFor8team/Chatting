package com.example.infinite.domain.artistcontent.post.artistpost.service.likecount;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ArtistPostLikeDeltaEventListener {

    private final ArtistPostLikeDeltaBuffer artistPostLikeDeltaBuffer;

    /**
     * AFTER_COMMIT 으로 둔 이유:
     * - Reaction insert/delete 가 실제 커밋된 경우에만 delta를 Redis에 적재하려는 목적
     * - 트랜잭션 롤백 시 count까지 잘못 증가/감소하는 것을 막는다
     *
     * 즉 "좋아요 원본"과 "좋아요 집계"의 경계를 가장 명확하게 나누는 지점이다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ArtistPostLikeDeltaEvent event) {
        // DB 트랜잭션이 실제로 커밋된 경우에만 Redis 누적값을 올려 롤백 이벤트가 count에 섞이지 않게 한다.
        artistPostLikeDeltaBuffer.accumulate(event.artistPostId(), event.delta());
    }
}
