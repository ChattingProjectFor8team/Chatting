package com.example.infinite.domain.artistcontent.comment.service.fanpost;

import com.example.infinite.domain.artistcontent.comment.dto.request.CommentCreateRequest;
import com.example.infinite.domain.artistcontent.comment.dto.response.CommentResponse;
import com.example.infinite.domain.artistcontent.comment.error.CommentErrorCode;
import com.example.infinite.domain.artistcontent.comment.error.CommentException;
import com.example.infinite.global.auth.MemberDetailsImpl;
import com.example.infinite.global.lock.LockException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FanPostCommentRedissonService {

    private final FanPostCommentCoreService fanPostCommentCoreService;
    private final FanPostCommentRedissonLockedService fanPostCommentRedissonLockedService;

    /**
     * FanPost 댓글은 기존 v1 동기 API 계약을 유지한다.
     * 다만 reply 생성/삭제처럼 같은 root thread에서 충돌 가능한 구간만 Redisson 락으로 감싼다.
     */
    public CommentResponse create(MemberDetailsImpl memberDetails, Long artistId, Long fanPostId, CommentCreateRequest request) {
        if (request.parentId() == null) {
            // root comment는 post 전체 락 없이도 병렬 생성 가능하다.
            return fanPostCommentCoreService.create(memberDetails, artistId, fanPostId, request);
        }

        Long rootCommentId = fanPostCommentCoreService.resolveReplyRootCommentId(fanPostId, request.parentId());
        try {
            return fanPostCommentRedissonLockedService.createReplyWithLock(
                    memberDetails,
                    artistId,
                    fanPostId,
                    request,
                    rootCommentId
            );
        } catch (LockException e) {
            throw new CommentException(CommentErrorCode.COMMENT_REQUEST_IN_PROGRESS);
        }
    }

    public void delete(MemberDetailsImpl memberDetails, Long artistId, Long fanPostId, Long commentId) {
        // root/reply 삭제 모두 최종적으로는 rootComment thread 하나에 영향을 주므로 같은 락 키로 정렬한다.
        Long rootCommentId = fanPostCommentCoreService.resolveRootCommentId(fanPostId, commentId);
        try {
            fanPostCommentRedissonLockedService.deleteWithLock(
                    memberDetails,
                    artistId,
                    fanPostId,
                    commentId,
                    rootCommentId
            );
        } catch (LockException e) {
            throw new CommentException(CommentErrorCode.COMMENT_REQUEST_IN_PROGRESS);
        }
    }
}
