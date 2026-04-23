package com.example.infinite.domain.artistcontent.interaction.service.fanletterlike;

import com.example.infinite.domain.artistcontent.interaction.dto.response.InteractionResponse;
import com.example.infinite.domain.artistcontent.interaction.entity.Reaction;
import com.example.infinite.domain.artistcontent.interaction.enums.ReactionType;
import com.example.infinite.domain.artistcontent.interaction.repository.InteractionRepository;
import com.example.infinite.domain.artistcontent.post.enums.PostType;
import com.example.infinite.domain.artistcontent.post.fanletter.repository.FanLetterRepository;
import com.example.infinite.domain.artistcontent.post.fanletter.support.FanLetterReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FanLetterLikeCoreService {

    private final InteractionRepository interactionRepository;
    private final FanLetterReader fanLetterReader;
    private final FanLetterRepository fanLetterRepository;

    /**
     * FanLetter 좋아요는 트래픽 규모상 즉시 처리로 충분하다.
     * 다만 count 컬럼은 entity ++ 대신 DB atomic update로 일관성을 맞춘다.
     */
    @Transactional
    public InteractionResponse toggle(Long memberId, Long artistId, Long fanLetterId) {
        fanLetterReader.findByIdAndArtistIdOrThrow(fanLetterId, artistId);

        return interactionRepository.findByTargetTypeAndTargetIdAndMemberIdAndReactionType(
                        PostType.FAN_LETTER,
                        fanLetterId,
                        memberId,
                        ReactionType.LIKE
                )
                .map(existingReaction -> cancelLike(existingReaction, fanLetterId))
                .orElseGet(() -> addLike(memberId, fanLetterId));
    }

    private InteractionResponse addLike(Long memberId, Long fanLetterId) {
        interactionRepository.save(Reaction.create(
                PostType.FAN_LETTER,
                fanLetterId,
                memberId,
                ReactionType.LIKE
        ));
        fanLetterRepository.changeLikeCountBy(fanLetterId, 1L);
        return InteractionResponse.of(fanLetterId, true, loadLikeCount(fanLetterId));
    }

    private InteractionResponse cancelLike(Reaction existingReaction, Long fanLetterId) {
        interactionRepository.delete(existingReaction);
        fanLetterRepository.changeLikeCountBy(fanLetterId, -1L);
        return InteractionResponse.of(fanLetterId, false, loadLikeCount(fanLetterId));
    }

    private long loadLikeCount(Long fanLetterId) {
        return fanLetterRepository.findLikeCountById(fanLetterId).orElse(0L);
    }
}
