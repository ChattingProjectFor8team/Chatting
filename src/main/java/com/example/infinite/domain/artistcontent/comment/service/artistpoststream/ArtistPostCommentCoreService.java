package com.example.infinite.domain.artistcontent.comment.service.artistpoststream;

import com.example.infinite.domain.artistcontent.comment.entity.Comment;
import com.example.infinite.domain.artistcontent.comment.repository.CommentRepository;
import com.example.infinite.domain.artistcontent.comment.service.CommentMentionService;
import com.example.infinite.domain.artistcontent.comment.support.CommentReader;
import com.example.infinite.domain.artistcontent.comment.support.MentionParser;
import com.example.infinite.domain.artistcontent.post.artistpost.support.ArtistPostReader;
import com.example.infinite.domain.artistcontent.post.enums.PostType;
import com.example.infinite.domain.member.member.entity.Member;
import com.example.infinite.domain.member.member.support.MemberReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArtistPostCommentCoreService {

    private final CommentRepository commentRepository;
    private final CommentReader commentReader;
    private final ArtistPostReader artistPostReader;
    private final CommentMentionService commentMentionService;
    private final MemberReader memberReader;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 댓글 원본 create/delete 비즈니스만 담당한다.
     *
     * 락과 stream 소비는 바깥 레이어가 맡고,
     * 여기서는 "DB에 댓글을 어떻게 기록/삭제할지"와 "count delta를 언제 발행할지"만 처리한다.
     */
    @Transactional
    public void createRoot(ArtistPostCommentStreamCommand command) {
        if (commentRepository.findByCommandRequestId(command.requestId()).isPresent()) {
            // stream 재처리로 같은 requestId가 다시 들어와도 중복 댓글을 만들지 않기 위한 idempotency check다.
            return;
        }

        artistPostReader.findByIdAndArtistIdOrThrow(command.artistPostId(), command.artistId());
        Member member = memberReader.findByIdOrThrow(command.memberId());

        Comment comment = commentRepository.save(Comment.create(
                PostType.ARTIST_POST,
                command.artistPostId(),
                member,
                command.content(),
                null,
                command.requestId()
        ));
        commentMentionService.syncMention(comment, null);
        publishDelta(command.artistPostId(), 1L);
    }

    @Transactional
    public void createReply(ArtistPostCommentStreamCommand command) {
        if (commentRepository.findByCommandRequestId(command.requestId()).isPresent()) {
            return;
        }

        artistPostReader.findByIdAndArtistIdOrThrow(command.artistPostId(), command.artistId());
        Member member = memberReader.findByIdOrThrow(command.memberId());
        Comment parentComment = commentReader.findByIdAndTargetTypeAndTargetIdOrThrow(
                command.parentCommentId(),
                PostType.ARTIST_POST,
                command.artistPostId()
        );
        if (!parentComment.isRootComment()) {
            log.warn("ArtistPost comment reply skipped: parent is not root, requestId={}", command.requestId());
            return;
        }

        String mentionNickname = resolveMentionNickname(command.content(), command.artistPostId(), parentComment);
        Comment comment = commentRepository.save(Comment.create(
                PostType.ARTIST_POST,
                command.artistPostId(),
                member,
                command.content(),
                parentComment,
                command.requestId()
        ));
        commentMentionService.syncMention(comment, mentionNickname);
        publishDelta(command.artistPostId(), 1L);
    }

    @Transactional
    public void delete(ArtistPostCommentStreamCommand command) {
        // delete는 idempotent하게 설계한다.
        // 이미 삭제됐거나 찾을 수 없는 댓글이면 no-op로 끝내 stream 재시도에도 버티게 한다.
        artistPostReader.findByIdAndArtistIdOrThrow(command.artistPostId(), command.artistId());
        Comment comment = commentRepository.findByIdAndTargetTypeAndTargetId(
                        command.commentId(),
                        PostType.ARTIST_POST,
                        command.artistPostId()
                )
                .orElse(null);
        if (comment == null) {
            return;
        }
        if (!comment.isOwnedBy(command.memberId())) {
            log.warn("ArtistPost comment delete skipped: owner mismatch, commentId={}, memberId={}",
                    command.commentId(), command.memberId());
            return;
        }
        if (comment.isDeletedPlaceholder()) {
            return;
        }

        commentMentionService.deleteMention(comment.getId());

        if (comment.isRootComment()) {
            if (!commentRepository.existsByParentId(comment.getId())) {
                comment.delete();
            } else {
                comment.markDeletedPlaceholder();
            }
            publishDelta(command.artistPostId(), -1L);
            return;
        }

        comment.delete();
        publishDelta(command.artistPostId(), -1L);

        Comment parentComment = comment.getParent();
        if (parentComment != null && parentComment.isDeletedPlaceholder() && !commentRepository.existsByParentId(parentComment.getId())) {
            parentComment.delete();
        }
    }

    public Long resolveRootCommentIdForDelete(Long artistPostId, Long commentId) {
        // 댓글 삭제 락 키는 root comment 기준이므로, reply 삭제도 먼저 root id를 계산해 thread lock을 맞춘다.
        Comment comment = commentRepository.findByIdAndTargetTypeAndTargetId(commentId, PostType.ARTIST_POST, artistPostId)
                .orElse(null);
        if (comment == null) {
            return null;
        }
        return comment.isRootComment() ? comment.getId() : comment.getParent().getId();
    }

    private void publishDelta(Long artistPostId, long delta) {
        applicationEventPublisher.publishEvent(new ArtistPostCommentDeltaEvent(artistPostId, delta));
    }

    private String resolveMentionNickname(String content, Long targetId, Comment parentComment) {
        // 기존 동기 댓글 정책과 동일하게 "같은 thread 참여자 중 첫 번째 유효 멘션 1건"만 해석한다.
        List<String> mentionedNicknames = MentionParser.extractMentionedNicknames(content);
        if (mentionedNicknames.isEmpty()) {
            return null;
        }

        List<Comment> threadComments = commentRepository.findThreadCommentsByRootCommentId(
                PostType.ARTIST_POST,
                targetId,
                parentComment.getId()
        );
        Set<String> allowedNicknames = threadComments.stream()
                .map(comment -> comment.getWriter().getNickname())
                .map(nickname -> nickname.strip().toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return mentionedNicknames.stream()
                .filter(allowedNicknames::contains)
                .findFirst()
                .orElse(null);
    }
}
