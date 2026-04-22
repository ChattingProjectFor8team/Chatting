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
        return fanPostCommentCoreService.create(memberDetails, artistId, fanPostId, request);
    }

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
