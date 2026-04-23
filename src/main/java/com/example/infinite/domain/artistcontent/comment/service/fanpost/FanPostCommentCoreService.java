package com.example.infinite.domain.artistcontent.comment.service.fanpost;

import com.example.infinite.domain.artistcontent.comment.dto.request.CommentCreateRequest;
import com.example.infinite.domain.artistcontent.comment.dto.response.CommentMentionResponse;
import com.example.infinite.domain.artistcontent.comment.dto.response.CommentResponse;
import com.example.infinite.domain.artistcontent.comment.entity.Comment;
import com.example.infinite.domain.artistcontent.comment.error.CommentErrorCode;
import com.example.infinite.domain.artistcontent.comment.error.CommentException;
import com.example.infinite.domain.artistcontent.comment.repository.CommentRepository;
import com.example.infinite.domain.artistcontent.comment.service.CommentMentionService;
import com.example.infinite.domain.artistcontent.comment.service.cache.CommentCacheInvalidationEvent;
import com.example.infinite.domain.artistcontent.comment.support.CommentReader;
import com.example.infinite.domain.artistcontent.comment.support.MentionParser;
import com.example.infinite.domain.artistcontent.post.enums.PostType;
import com.example.infinite.domain.artistcontent.post.fanpost.repository.FanPostRepository;
import com.example.infinite.domain.artistcontent.post.fanpost.support.FanPostReader;
import com.example.infinite.domain.member.member.entity.Member;
import com.example.infinite.domain.member.member.support.MemberInputSupport;
import com.example.infinite.domain.member.member.support.MemberReader;
import com.example.infinite.domain.subscriptionmembership.dto.response.WriterSubscriptionBadge;
import com.example.infinite.domain.subscriptionmembership.service.SubscriptionMembershipService;
import com.example.infinite.global.auth.MemberDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FanPostCommentCoreService {

    private final CommentRepository commentRepository;
    private final CommentReader commentReader;
    private final FanPostReader fanPostReader;
    private final FanPostRepository fanPostRepository;
    private final CommentMentionService commentMentionService;
    private final MemberReader memberReader;
    private final SubscriptionMembershipService subscriptionMembershipService;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * FanPost 댓글은 기존 동기 계약을 유지하되, count 갱신은 atomic update로 바꾼다.
     * 락은 reply 생성/삭제처럼 thread 충돌이 있는 지점에서만 바깥 서비스가 담당한다.
     */
    @Transactional
    public CommentResponse create(MemberDetailsImpl memberDetails, Long artistId, Long fanPostId, CommentCreateRequest request) {
        // 작성자와 대상 글을 먼저 확정해야
        // 이후의 parent 검증 / mention 해석 / count 반영이 모두 같은 기준 위에서 동작한다.
        Member member = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));
        fanPostReader.findByIdAndArtistIdOrThrow(fanPostId, artistId);

        // parent 는 depth 2 정책을 지키는지 확인하는 핵심 지점이다.
        Comment parentComment = resolveParentComment(request.parentId(), fanPostId);
        // mention 은 "대댓글에서만, 같은 thread 참여자만" 허용되는 규칙을 여기서 해석한다.
        String mentionNickname = resolveMentionNickname(request.content(), fanPostId, parentComment);

        Comment comment = commentRepository.save(Comment.create(
                PostType.FAN_POST,
                fanPostId,
                member,
                request.content(),
                parentComment
        ));
        // count 는 엔티티를 다시 읽어 변경하지 않고 atomic update 로 바로 반영해
        // 같은 게시글에 댓글이 몰릴 때 lost update 위험을 줄인다.
        fanPostRepository.changeCommentCountBy(fanPostId, 1L);
        publishCommentCacheInvalidation(PostType.FAN_POST, fanPostId, parentComment == null ? null : parentComment.getId());

        CommentMentionResponse mentionedMember = commentMentionService.syncMention(comment, mentionNickname);
        WriterSubscriptionBadge writerBadge = loadSingleWriterBadge(artistId, member.getId());
        return CommentResponse.from(
                comment,
                writerBadge.fanMembershipSubscribed(),
                writerBadge.dmSubscribed(),
                mentionedMember
        );
    }

    @Transactional
    public void delete(MemberDetailsImpl memberDetails, Long artistId, Long fanPostId, Long commentId) {
        Member member = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));
        fanPostReader.findByIdAndArtistIdOrThrow(fanPostId, artistId);
        Comment comment = commentReader.findByIdAndTargetTypeAndTargetIdOrThrow(commentId, PostType.FAN_POST, fanPostId);

        if (!comment.isOwnedBy(member.getId())) {
            throw new CommentException(CommentErrorCode.COMMENT_PERMISSION_DENIED);
        }
        if (comment.isDeletedPlaceholder()) {
            // 이미 "삭제된 댓글" placeholder 인 경우 다시 count 를 건드리면 안 된다.
            return;
        }

        commentMentionService.deleteMention(comment.getId());

        if (comment.isRootComment()) {
            if (!commentRepository.existsByParentId(comment.getId())) {
                // 자식이 하나도 없으면 부모 댓글 자체를 그냥 soft delete 한다.
                comment.delete();
            } else {
                // 자식이 남아 있으면 문맥 유지를 위해 placeholder 로만 바꾼다.
                comment.markDeletedPlaceholder();
            }
            fanPostRepository.changeCommentCountBy(fanPostId, -1L);
            publishCommentCacheInvalidation(PostType.FAN_POST, fanPostId, comment.getId());
            return;
        }

        // reply 는 바로 soft delete 하고 count 를 감소시킨다.
        comment.delete();
        fanPostRepository.changeCommentCountBy(fanPostId, -1L);

        Comment parentComment = comment.getParent();
        if (parentComment != null && parentComment.isDeletedPlaceholder() && !commentRepository.existsByParentId(parentComment.getId())) {
            // placeholder 부모에 달린 마지막 reply 도 사라졌다면
            // 이제 부모를 실제 soft delete 로 마무리할 수 있다.
            parentComment.delete();
        }
        publishCommentCacheInvalidation(PostType.FAN_POST, fanPostId, parentComment == null ? null : parentComment.getId());
    }

    public Long resolveRootCommentId(Long fanPostId, Long commentId) {
        // 삭제 시 현재 댓글이 root 인지 reply 인지에 따라
        // 어떤 thread lock 키를 써야 하는지 계산한다.
        Comment comment = commentReader.findByIdAndTargetTypeAndTargetIdOrThrow(commentId, PostType.FAN_POST, fanPostId);
        return comment.isRootComment() ? comment.getId() : comment.getParent().getId();
    }

    public Long resolveReplyRootCommentId(Long fanPostId, Long parentCommentId) {
        return resolveParentComment(parentCommentId, fanPostId).getId();
    }

    private Comment resolveParentComment(Long parentId, Long fanPostId) {
        if (parentId == null) {
            return null;
        }

        // parent 는 반드시 같은 글의 root comment 여야 한다.
        // reply 아래에 reply 를 다는 순간 3-depth 가 되므로 금지한다.
        Comment parentComment = commentReader.findByIdAndTargetTypeAndTargetIdOrThrow(parentId, PostType.FAN_POST, fanPostId);
        if (!parentComment.isRootComment()) {
            throw new CommentException(CommentErrorCode.COMMENT_DEPTH_EXCEEDED);
        }
        return parentComment;
    }

    private String resolveMentionNickname(String content, Long fanPostId, Comment parentComment) {
        List<String> mentionedNicknames = MentionParser.extractMentionedNicknames(content);
        if (mentionedNicknames.isEmpty() || parentComment == null) {
            // 원댓글에서는 mention 을 해석하지 않고, 대댓글이 아닐 때도 그냥 일반 텍스트로 둔다.
            return null;
        }

        List<Comment> threadComments = commentRepository.findThreadCommentsByRootCommentId(
                PostType.FAN_POST,
                fanPostId,
                parentComment.getId()
        );
        Set<String> allowedNicknames = threadComments.stream()
                .map(comment -> comment.getWriter().getNickname())
                .map(nickname -> nickname.strip().toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // 여러 @문자가 있어도 "같은 thread 참여자에 해당하는 첫 번째 한 명"만 인정한다.
        return mentionedNicknames.stream()
                .filter(allowedNicknames::contains)
                .findFirst()
                .orElse(null);
    }

    private WriterSubscriptionBadge loadSingleWriterBadge(Long artistId, Long writerId) {
        return subscriptionMembershipService.getWriterBadges(artistId, List.of(writerId))
                .getOrDefault(writerId, WriterSubscriptionBadge.empty(writerId));
    }

    private void publishCommentCacheInvalidation(PostType targetType, Long targetId, Long rootCommentId) {
        applicationEventPublisher.publishEvent(new CommentCacheInvalidationEvent(targetType, targetId, rootCommentId));
    }
}
