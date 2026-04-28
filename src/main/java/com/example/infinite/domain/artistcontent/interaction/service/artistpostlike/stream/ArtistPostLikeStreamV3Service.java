package com.example.infinite.domain.artistcontent.interaction.service.artistpostlike.stream;

import com.example.infinite.domain.artistcontent.interaction.dto.response.ArtistPostLikeQueuedResponse;
import com.example.infinite.domain.artistcontent.interaction.error.InteractionErrorCode;
import com.example.infinite.domain.artistcontent.interaction.error.InteractionException;
import com.example.infinite.global.lock.LockException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ArtistPostLikeStreamV3Service {

    private final ArtistPostLikeRedissonV3LockedService artistPostLikeRedissonV3LockedService;

    /**
     * 컨트롤러가 호출하는 V3 좋아요 진입 서비스다.
     *
     * 역할을 얇게 둔 이유:
     * - 실제 동시성 제어는 lock service가 담당
     * - 여기서는 "락 충돌을 어떤 API 에러로 바꿀지"만 결정
     *
     * 즉 controller -> v3 service -> locked service 로 층을 나눠
     * HTTP 계약과 락 처리 로직을 섞지 않게 만든다.
     */
    public ArtistPostLikeQueuedResponse queue(Long memberId, Long artistId, Long artistPostId) {
        try {
            return artistPostLikeRedissonV3LockedService.queueWithLock(memberId, artistId, artistPostId);
        } catch (LockException e) {
            // 같은 member + post 요청이 아직 처리 중이면 "이미 진행 중" 에러로 바꿔
            // 클라이언트가 중복 클릭/재시도를 해석할 수 있게 한다.
            throw new InteractionException(InteractionErrorCode.LIKE_REQUEST_IN_PROGRESS);
        }
    }
}
