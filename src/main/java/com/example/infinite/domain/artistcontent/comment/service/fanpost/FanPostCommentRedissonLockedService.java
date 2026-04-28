package com.example.infinite.domain.artistcontent.comment.service.fanpost;

import com.example.infinite.domain.artistcontent.comment.dto.request.CommentCreateRequest;
import com.example.infinite.domain.artistcontent.comment.dto.response.CommentResponse;
import com.example.infinite.global.auth.MemberDetailsImpl;
import com.example.infinite.global.lock.RedisLock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class FanPostCommentRedissonLockedService {

    private final FanPostCommentCoreService fanPostCommentCoreService;

    /**
     * 대댓글 생성은 root thread 단위 락 안에서만 수행한다.
     *
     * 왜 rootCommentId 로 잠그는가:
     * - 같은 스레드 안에서는 reply 생성/삭제/placeholder 처리 순서가 중요
     * - 하지만 서로 다른 루트 댓글 스레드까지 함께 막을 필요는 없음
     * - 그래서 "post 전체 락"보다 "thread 락"이 훨씬 덜 비싸다
     */
    @RedisLock(
            key = "'fan-post:comment-thread:' + #rootCommentId",
            waitTime = 1,
            leaseTime = 5,
            timeUnit = TimeUnit.SECONDS
    )
    public CommentResponse createReplyWithLock(
            MemberDetailsImpl memberDetails,
            Long artistId,
            Long fanPostId,
            CommentCreateRequest request,
            Long rootCommentId
    ) {
        // 락을 잡은 상태에서 실제 생성은 core service에 위임해
        // 비즈니스 로직과 락 기술 코드를 분리한다.
        return fanPostCommentCoreService.create(memberDetails, artistId, fanPostId, request);
    }

    /**
     * 삭제도 같은 root thread 기준으로 직렬화한다.
     * 그래야 "마지막 reply 삭제 -> placeholder parent 정리" 같은 후처리가 꼬이지 않는다.
     */
    @RedisLock(
            key = "'fan-post:comment-thread:' + #rootCommentId",
            waitTime = 1,
            leaseTime = 5,
            timeUnit = TimeUnit.SECONDS
    )
    public void deleteWithLock(
            MemberDetailsImpl memberDetails,
            Long artistId,
            Long fanPostId,
            Long commentId,
            Long rootCommentId
    ) {
        fanPostCommentCoreService.delete(memberDetails, artistId, fanPostId, commentId);
    }
}
