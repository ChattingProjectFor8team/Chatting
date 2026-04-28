package com.example.infinite.domain.artistcontent.interaction.service.commentlike;

import com.example.infinite.domain.artistcontent.interaction.dto.response.InteractionResponse;
import com.example.infinite.domain.artistcontent.post.enums.PostType;
import com.example.infinite.global.lock.RedisLock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CommentLikeRedissonLockedService {

    private final CommentLikeCoreService commentLikeCoreService;

    /**
     * 댓글 좋아요는 같은 member + comment 조합만 직렬화한다.
     * 댓글이 달린 게시글 전체를 잠그지 않으므로 서로 다른 유저/댓글 요청은 계속 병렬 처리된다.
     */
    @RedisLock(
            key = "'comment:like:' + #commentId + ':member:' + #memberId",
            waitTime = 700,
            leaseTime = 3000,
            timeUnit = TimeUnit.MILLISECONDS
    )
    public InteractionResponse toggleWithLock(
            Long memberId,
            Long artistId,
            Long targetId,
            Long commentId,
            PostType targetType
    ) {
        return commentLikeCoreService.toggle(memberId, artistId, targetId, commentId, targetType);
    }
}
