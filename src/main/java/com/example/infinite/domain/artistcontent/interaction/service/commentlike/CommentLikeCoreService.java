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
        // 댓글 좋아요는 "댓글이 속한 원글"이 유효한지와 "댓글 자체"가 유효한지를 둘 다 확인해야 한다.
        // targetType/targetId 검증은 댓글이 올바른 게시글 문맥 안에 있는지 확인하는 단계다.
        validateTarget(targetType, artistId, targetId);
        Comment comment = commentReader.findByIdAndTargetTypeAndTargetIdOrThrow(commentId, targetType, targetId);

        // Reaction의 실제 타깃은 FAN_POST / ARTIST_POST가 아니라 COMMENT다.
        // 즉 "어느 글의 댓글인지"는 검증용 문맥이고, 좋아요 row는 commentId 기준으로 저장된다.
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
        // 댓글 좋아요 API는 현재 FanPost, ArtistPost의 댓글만 지원한다.
        // 원글을 먼저 검증해 두면 다른 글에 속한 commentId를 잘못 주는 요청도 초기에 걸러진다.
        switch (targetType) {
            case FAN_POST -> fanPostReader.findByIdAndArtistIdOrThrow(targetId, artistId);
            case ARTIST_POST -> artistPostReader.findByIdAndArtistIdOrThrow(targetId, artistId);
            default -> throw new IllegalArgumentException("지원하지 않는 댓글 대상 타입입니다: " + targetType);
        }
    }
}
