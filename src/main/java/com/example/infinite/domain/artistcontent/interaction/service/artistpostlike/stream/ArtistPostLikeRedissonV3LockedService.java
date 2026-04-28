package com.example.infinite.domain.artistcontent.interaction.service.artistpostlike.stream;

import com.example.infinite.domain.artistcontent.interaction.dto.response.ArtistPostLikeQueuedResponse;
import com.example.infinite.domain.artistcontent.interaction.enums.ReactionType;
import com.example.infinite.domain.artistcontent.interaction.repository.InteractionRepository;
import com.example.infinite.domain.artistcontent.post.artistpost.support.ArtistPostReader;
import com.example.infinite.domain.artistcontent.post.enums.PostType;
import com.example.infinite.global.lock.RedisLock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ArtistPostLikeRedissonV3LockedService {

    private final ArtistPostReader artistPostReader;
    private final InteractionRepository interactionRepository;
    private final ArtistPostLikePendingStateRepository pendingStateRepository;
    private final ArtistPostLikeStreamProducer streamProducer;

    /**
     * V3 요청 진입점.
     *
     * 흐름:
     * 1. member + post 단위 락 획득
     * 2. DB 상태 + pending state를 합쳐 "현재 유저의 실질 상태" 계산
     * 3. desired state를 뒤집어 stream command 적재
     * 4. 즉시 202 응답
     *
     * 락 범위를 member + post로 좁힌 이유는 V2와 동일하다.
     * 인기 글 하나를 post 단위로 잠그면 1만 명 요청이 전부 직렬화되기 때문이다.
     */
    @RedisLock(
            key = "'artist-post:like:v3:' + #artistPostId + ':member:' + #memberId",
            waitTime = 700,
            leaseTime = 3000,
            timeUnit = TimeUnit.MILLISECONDS
    )
    public ArtistPostLikeQueuedResponse queueWithLock(Long memberId, Long artistId, Long artistPostId) {
        // 같은 member-target 요청만 직렬화해 "현재 유저 의도 상태"를 안전하게 토글하고 stream에 적재한다.
        artistPostReader.findByIdAndArtistIdOrThrow(artistPostId, artistId);

        boolean effectiveReacted = pendingStateRepository.findPendingDesiredState(artistPostId, memberId)
                .orElseGet(() -> interactionRepository.existsByTargetTypeAndTargetIdAndMemberIdAndReactionType(
                        PostType.ARTIST_POST,
                        artistPostId,
                        memberId,
                        ReactionType.LIKE
                ));
        boolean desiredReacted = !effectiveReacted;
        // pending state는 stream consumer가 DB 반영을 끝낼 때까지 요청 경로가 참고하는 "가상 최신 상태"다.
        long pendingVersion = pendingStateRepository.nextPendingVersion(artistPostId, memberId);
        String requestId = UUID.randomUUID().toString();
        pendingStateRepository.savePendingDesiredState(artistPostId, memberId, desiredReacted);

        try {
            streamProducer.enqueue(ArtistPostLikeStreamCommand.create(
                    requestId,
                    artistId,
                    artistPostId,
                    memberId,
                    desiredReacted,
                    pendingVersion
            ));
        } catch (RuntimeException e) {
            // pending state를 먼저 저장하는 구조라 enqueue 실패 시 정리를 해주지 않으면
            // 실제 DB에는 반영되지 않았는데도 다음 요청이 "가상 최신 상태"를 계속 바라보게 된다.
            pendingStateRepository.clearPendingStateIfVersionMatches(artistPostId, memberId, pendingVersion);
            throw e;
        }

        return new ArtistPostLikeQueuedResponse(requestId, artistPostId, desiredReacted, LocalDateTime.now());
    }
}
