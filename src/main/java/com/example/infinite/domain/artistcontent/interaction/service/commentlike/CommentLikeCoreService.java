package com.example.infinite.domain.artistcontent.interaction.service.commentlike;

import com.example.infinite.domain.artistcontent.comment.entity.Comment;
import com.example.infinite.domain.artistcontent.comment.repository.CommentRepository;
import com.example.infinite.domain.artistcontent.comment.support.CommentReader;
import com.example.infinite.domain.artistcontent.interaction.dto.response.InteractionResponse;
import com.example.infinite.domain.artistcontent.interaction.entity.Reaction;
import com.example.infinite.domain.artistcontent.interaction.enums.ReactionType;
import com.example.infinite.domain.artistcontent.interaction.repository.InteractionRepository;
import com.example.infinite.domain.artistcontent.post.artistpost.support.ArtistPostReader;
import com.example.infinite.domain.artistcontent.post.enums.PostType;
import com.example.infinite.domain.artistcontent.post.fanpost.support.FanPostReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentLikeCoreService {

    private final InteractionRepository interactionRepository;
    private final CommentRepository commentRepository;
    private final CommentReader commentReader;
    private final FanPostReader fanPostReader;
    private final ArtistPostReader artistPostReader;

    /**
     * 댓글 좋아요는 target post와 comment를 먼저 검증하고,
     * 실제 count 반영은 엔티티 ++ 대신 DB atomic update로 처리한다.
     *
     * 댓글도 좋아요 수가 몰릴 수 있어서 in-memory increment로 두면
     * lost update가 다시 발생한다.
     */
    @Transactional
    public InteractionResponse toggle(Long memberId, Long artistId, Long targetId, Long commentId, PostType targetType) {
        validateTarget(targetType, artistId, targetId);
        Comment comment = commentReader.findByIdAndTargetTypeAndTargetIdOrThrow(commentId, targetType, targetId);

        return interactionRepository.findByTargetTypeAndTargetIdAndMemberIdAndReactionType(
                        PostType.COMMENT,
                        comment.getId(),
                        memberId,
                        ReactionType.LIKE
                )
                .map(existingReaction -> cancelLike(existingReaction, comment.getId()))
                .orElseGet(() -> addLike(memberId, comment.getId()));
    }

    private InteractionResponse addLike(Long memberId, Long commentId) {
        interactionRepository.save(Reaction.create(
                PostType.COMMENT,
                commentId,
                memberId,
                ReactionType.LIKE
        ));
        commentRepository.changeLikeCountBy(commentId, 1L);
        return InteractionResponse.of(commentId, true, loadLikeCount(commentId));
    }

    private InteractionResponse cancelLike(Reaction existingReaction, Long commentId) {
        interactionRepository.delete(existingReaction);
        commentRepository.changeLikeCountBy(commentId, -1L);
        return InteractionResponse.of(commentId, false, loadLikeCount(commentId));
    }

    private long loadLikeCount(Long commentId) {
        return commentRepository.findLikeCountById(commentId).orElse(0L);
    }

    private void validateTarget(PostType targetType, Long artistId, Long targetId) {
        switch (targetType) {
            case FAN_POST -> fanPostReader.findByIdAndArtistIdOrThrow(targetId, artistId);
            case ARTIST_POST -> artistPostReader.findByIdAndArtistIdOrThrow(targetId, artistId);
            default -> throw new IllegalArgumentException("지원하지 않는 댓글 대상 타입입니다: " + targetType);
        }
    }
}
