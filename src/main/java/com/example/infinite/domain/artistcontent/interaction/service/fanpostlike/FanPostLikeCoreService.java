package com.example.infinite.domain.artistcontent.interaction.service.fanpostlike;

import com.example.infinite.domain.artistcontent.interaction.dto.response.InteractionResponse;
import com.example.infinite.domain.artistcontent.interaction.entity.Reaction;
import com.example.infinite.domain.artistcontent.interaction.enums.ReactionType;
import com.example.infinite.domain.artistcontent.interaction.repository.InteractionRepository;
import com.example.infinite.domain.artistcontent.post.eunms.PostType;
import com.example.infinite.domain.artistcontent.post.fanpost.repository.FanPostRepository;
import com.example.infinite.domain.artistcontent.post.fanpost.support.FanPostReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FanPostLikeCoreService {

    private final InteractionRepository interactionRepository;
    private final FanPostReader fanPostReader;
    private final FanPostRepository fanPostRepository;

    /**
     * FanPost 좋아요 핵심 비즈니스.
     * 저트래픽 영역이라 stream까지는 쓰지 않고, Redisson 락 + atomic update로 단순하게 정리한다.
     */
    @Transactional
    public InteractionResponse toggle(Long memberId, Long artistId, Long fanPostId) {
        fanPostReader.findByIdAndArtistIdOrThrow(fanPostId, artistId);

        return interactionRepository.findByTargetTypeAndTargetIdAndMemberIdAndReactionType(
                        PostType.FAN_POST,
                        fanPostId,
                        memberId,
                        ReactionType.LIKE
                )
                .map(existingReaction -> cancelLike(existingReaction, fanPostId))
                .orElseGet(() -> addLike(memberId, fanPostId));
    }

    private InteractionResponse addLike(Long memberId, Long fanPostId) {
        interactionRepository.save(Reaction.create(
                PostType.FAN_POST,
                fanPostId,
                memberId,
                ReactionType.LIKE
        ));
        fanPostRepository.changeLikeCountBy(fanPostId, 1L);
        return InteractionResponse.of(fanPostId, true, loadLikeCount(fanPostId));
    }

    private InteractionResponse cancelLike(Reaction existingReaction, Long fanPostId) {
        interactionRepository.delete(existingReaction);
        fanPostRepository.changeLikeCountBy(fanPostId, -1L);
        return InteractionResponse.of(fanPostId, false, loadLikeCount(fanPostId));
    }

    private long loadLikeCount(Long fanPostId) {
        return fanPostRepository.findLikeCountById(fanPostId).orElse(0L);
    }
}
