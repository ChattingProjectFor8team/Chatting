package com.example.infinite.domain.artistcontent.interaction.service.artistpostlike;

import com.example.infinite.domain.artistcontent.interaction.dto.response.InteractionResponse;
import com.example.infinite.domain.artistcontent.interaction.error.InteractionErrorCode;
import com.example.infinite.domain.artistcontent.interaction.error.InteractionException;
import com.example.infinite.global.lock.LockException;
import com.example.infinite.global.lock.lettuce.LettuceLockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ArtistPostLikeLettuceV1Service {

    private static final long ARTIST_POST_LIKE_LOCK_WAIT_MS = 700L;
    private static final long ARTIST_POST_LIKE_LOCK_LEASE_MS = 3_000L;

    private final LettuceLockService lettuceLockService;
    private final ArtistPostLikeCoreService artistPostLikeCoreService;

    /**
     * V1: Lettuce 기반 수동 락 버전.
     *
     * 현재 실사용 경로는 아니고, 아래 목적을 위해 유지한다.
     * - 과제 발표에서 "Lettuce로 직접 락을 구현한 버전" 설명
     * - Redisson V2와의 비교 기준
     * - 필요 시 AOP 없이도 락을 명시적으로 제어하는 예시
     *
     * 락 범위를 memberId + artistPostId로 잡은 이유:
     * - 같은 유저가 같은 글에 중복 토글하는 것만 직렬화하면 된다.
     * - post 전체를 잠그면 인기 글에서 모든 요청이 일렬로 서서 병목이 된다.
     */
    public InteractionResponse toggle(Long memberId, Long artistId, Long artistPostId) {
        String lockKey = buildLockKey(artistPostId, memberId);
        boolean locked = false;

        try {
            // waitTime 동안 스핀락으로 재시도하고, leaseTime 동안만 락을 점유한다.
            // 서버 장애나 예외로 unlock이 누락돼도 TTL이 지나면 자동 해제되게 한 구조다.
            lettuceLockService.lock(
                    lockKey,
                    ARTIST_POST_LIKE_LOCK_WAIT_MS,
                    ARTIST_POST_LIKE_LOCK_LEASE_MS,
                    TimeUnit.MILLISECONDS
            );
            locked = true;
            return artistPostLikeCoreService.toggle(memberId, artistId, artistPostId);
        } catch (LockException e) {
            // 락 대기 시간 안에 못 잡았으면 "처리 중" 에러로 빠르게 실패시킨다.
            throw new InteractionException(InteractionErrorCode.LIKE_REQUEST_IN_PROGRESS);
        } finally {
            if (locked) {
                // 직접 락을 다루는 버전이라 성공한 경우에만 명시적으로 해제한다.
                lettuceLockService.unlock(lockKey);
            }
        }
    }

    private String buildLockKey(Long artistPostId, Long memberId) {
        // member + target 단위로 잘게 쪼개야 서로 다른 유저 요청은 병렬로 흘러간다.
        return "artist-post:like:" + artistPostId + ":member:" + memberId;
    }
}
