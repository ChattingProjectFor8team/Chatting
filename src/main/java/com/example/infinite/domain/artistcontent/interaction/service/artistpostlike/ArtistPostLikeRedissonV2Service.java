package com.example.infinite.domain.artistcontent.interaction.service.artistpostlike;

import com.example.infinite.domain.artistcontent.interaction.dto.response.InteractionResponse;
import com.example.infinite.domain.artistcontent.interaction.error.InteractionErrorCode;
import com.example.infinite.domain.artistcontent.interaction.error.InteractionException;
import com.example.infinite.global.lock.LockException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ArtistPostLikeRedissonV2Service {

    private final ArtistPostLikeRedissonV2LockedService artistPostLikeRedissonV2LockedService;

    /**
     * V2 실사용 facade.
     *
     * 역할:
     * - 실제 운영 경로에서 호출되는 진입점
     * - Redisson 락 예외를 도메인 예외로 변환
     *
     * 락 구현 세부사항을 InteractionService가 몰라도 되게 하려고 한 겹 감싼다.
     */
    public InteractionResponse toggle(Long memberId, Long artistId, Long artistPostId) {
        try {
            return artistPostLikeRedissonV2LockedService.toggleWithLock(memberId, artistId, artistPostId);
        } catch (LockException e) {
            // 운영 레이어에서는 LockException 자체보다 "좋아요 처리 중"이라는 도메인 의미가 더 중요하다.
            throw new InteractionException(InteractionErrorCode.LIKE_REQUEST_IN_PROGRESS);
        }
    }
}
