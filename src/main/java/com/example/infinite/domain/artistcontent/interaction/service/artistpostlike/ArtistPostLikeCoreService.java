package com.example.infinite.domain.artistcontent.interaction.service.artistpostlike;

import com.example.infinite.domain.artistcontent.interaction.dto.response.InteractionResponse;
import com.example.infinite.domain.artistcontent.interaction.entity.Reaction;
import com.example.infinite.domain.artistcontent.interaction.enums.ReactionType;
import com.example.infinite.domain.artistcontent.interaction.repository.InteractionRepository;
import com.example.infinite.domain.artistcontent.post.artistpost.entity.ArtistPost;
import com.example.infinite.domain.artistcontent.post.artistpost.service.likecount.ArtistPostLikeDeltaEvent;
import com.example.infinite.domain.artistcontent.post.artistpost.support.ArtistPostReader;
import com.example.infinite.domain.artistcontent.post.eunms.PostType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArtistPostLikeCoreService {

    private final InteractionRepository interactionRepository;
    private final ArtistPostReader artistPostReader;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * ArtistPost 좋아요의 "핵심 비즈니스"만 담당한다.
     *
     * 이 서비스가 하는 일:
     * - 대상 ArtistPost 검증
     * - Reaction insert/delete
     * - after-commit delta 이벤트 발행
     *
     * 이 서비스가 하지 않는 일:
     * - 어떤 락 구현을 쓸지 결정
     * - Lettuce/Redisson 같은 인프라 세부사항 처리
     *
     * 이렇게 분리한 이유는 락 전략(V1, V2)이 바뀌어도 실제 좋아요 비즈니스는
     * 한 군데에서 유지되게 하려는 것이다.
     */
    @Transactional
    public InteractionResponse toggle(Long memberId, Long artistId, Long artistPostId) {
        ArtistPost artistPost = artistPostReader.findByIdAndArtistIdOrThrow(artistPostId, artistId);

        return interactionRepository.findByTargetTypeAndTargetIdAndMemberIdAndReactionType(
                        PostType.ARTIST_POST,
                        artistPostId,
                        memberId,
                        ReactionType.LIKE
                )
                .map(existingReaction -> cancelLike(existingReaction, artistPost))
                .orElseGet(() -> addLike(memberId, artistPost));
    }

    private InteractionResponse addLike(Long memberId, ArtistPost artistPost) {
        // "좋아요를 눌렀다"는 원본 사실은 Reaction 테이블에 즉시 저장한다.
        // 반면 artist_posts.like_count는 파생 집계값이므로 여기서 직접 증가시키지 않는다.
        //
        // 이유:
        // - 고트래픽 글에서는 like_count 한 row를 요청마다 갱신하면 row hotspot이 생긴다.
        // - 원본과 집계값을 분리하면 "요청 처리 속도"와 "화면 집계값 정합성"을 따로 관리할 수 있다.
        interactionRepository.save(Reaction.create(
                PostType.ARTIST_POST,
                artistPost.getId(),
                memberId,
                ReactionType.LIKE
        ));
        publishArtistPostLikeDelta(artistPost.getId(), 1L);
        return InteractionResponse.of(artistPost.getId(), true, estimateArtistPostLikeCount(artistPost, 1L));
    }

    private InteractionResponse cancelLike(Reaction existingReaction, ArtistPost artistPost) {
        // 좋아요 취소도 addLike와 동일한 원리다.
        // 원본 Reaction만 즉시 지우고, likeCount 감소는 delta 이벤트로 넘긴다.
        interactionRepository.delete(existingReaction);
        publishArtistPostLikeDelta(artistPost.getId(), -1L);
        return InteractionResponse.of(artistPost.getId(), false, estimateArtistPostLikeCount(artistPost, -1L));
    }

    private void publishArtistPostLikeDelta(Long artistPostId, long delta) {
        // 이벤트는 "트랜잭션 안에서" 발행하지만,
        // 실제 Redis 누적은 AFTER_COMMIT 리스너가 처리한다.
        // 즉 DB 저장이 롤백되면 delta도 집계되지 않는다.
        applicationEventPublisher.publishEvent(new ArtistPostLikeDeltaEvent(artistPostId, delta));
    }

    private long estimateArtistPostLikeCount(ArtistPost artistPost, long delta) {
        // 응답은 사용자의 직전 토글 효과만 반영한 추정치로 반환하고, 최종 count는 flush/reconcile 배치가 맞춘다.
        return Math.max(0L, artistPost.getLikeCount() + delta);
    }
}
