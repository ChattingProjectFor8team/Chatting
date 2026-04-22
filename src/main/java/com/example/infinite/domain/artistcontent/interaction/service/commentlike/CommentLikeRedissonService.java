package com.example.infinite.domain.artistcontent.interaction.service.commentlike;

import com.example.infinite.domain.artistcontent.interaction.dto.response.InteractionResponse;
import com.example.infinite.domain.artistcontent.interaction.error.InteractionErrorCode;
import com.example.infinite.domain.artistcontent.interaction.error.InteractionException;
import com.example.infinite.domain.artistcontent.post.enums.PostType;
import com.example.infinite.global.lock.LockException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommentLikeRedissonService {

    private final CommentLikeRedissonLockedService commentLikeRedissonLockedService;

    public InteractionResponse toggle(Long memberId, Long artistId, Long targetId, Long commentId, PostType targetType) {
        try {
            return commentLikeRedissonLockedService.toggleWithLock(
                    memberId,
                    artistId,
                    targetId,
                    commentId,
                    targetType
            );
        } catch (LockException e) {
            throw new InteractionException(InteractionErrorCode.LIKE_REQUEST_IN_PROGRESS);
        }
    }
}
