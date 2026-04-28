package com.example.infinite.domain.artistcontent.comment.service.artistpoststream;

import com.example.infinite.global.lock.RedisLock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ArtistPostCommentThreadLockedService {

    private final ArtistPostCommentCoreService artistPostCommentCoreService;

    /**
     * 댓글에서 락이 필요한 지점은 post 전체가 아니라 root thread다.
     * 서로 다른 루트 댓글 스레드는 병렬 허용하고, 같은 스레드 안의 생성/삭제 충돌만 직렬화한다.
     */
    @RedisLock(
            key = "'artist-post:comment-thread:' + #rootCommentId",
            waitTime = 1,
            leaseTime = 5,
            timeUnit = TimeUnit.SECONDS
    )
    public void createReplyWithLock(ArtistPostCommentStreamCommand command, Long rootCommentId) {
        // 대댓글 생성은 같은 스레드 안의 삭제/placeholder 처리와 충돌할 수 있어 root thread 단위로 잠근다.
        artistPostCommentCoreService.createReply(command);
    }

    @RedisLock(
            key = "'artist-post:comment-thread:' + #rootCommentId",
            waitTime = 1,
            leaseTime = 5,
            timeUnit = TimeUnit.SECONDS
    )
    public void deleteWithLock(ArtistPostCommentStreamCommand command, Long rootCommentId) {
        // 삭제도 root thread 단위로 직렬화해야 자식 존재 여부 판단과 placeholder 정리가 꼬이지 않는다.
        artistPostCommentCoreService.delete(command);
    }
}
