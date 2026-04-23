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

    /**
     * 컨트롤러/상위 서비스가 실제로 호출하는 댓글 좋아요 진입점이다.
     *
     * 이 계층을 따로 두는 이유:
     * - locked service는 "락을 건 상태에서 실행"하는 데 집중
     * - 여기서는 LockException을 API 의미가 있는 도메인 예외로 번역
     *
     * 즉 AOP 락 기술 세부사항이 바깥 계층으로 새지 않게 막는 얇은 facade다.
     */
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
            // 사용자 입장에서는 "락 구현 실패"보다 "같은 좋아요 요청이 아직 처리 중"이라는 의미가 중요하다.
            throw new InteractionException(InteractionErrorCode.LIKE_REQUEST_IN_PROGRESS);
        }
    }
}
