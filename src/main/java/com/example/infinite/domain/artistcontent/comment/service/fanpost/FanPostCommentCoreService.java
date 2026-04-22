package com.example.infinite.domain.artistcontent.comment.service.fanpost;

import com.example.infinite.domain.artistcontent.comment.dto.request.CommentCreateRequest;
import com.example.infinite.domain.artistcontent.comment.dto.response.CommentMentionResponse;
import com.example.infinite.domain.artistcontent.comment.dto.response.CommentResponse;
import com.example.infinite.domain.artistcontent.comment.entity.Comment;
import com.example.infinite.domain.artistcontent.comment.error.CommentErrorCode;
import com.example.infinite.domain.artistcontent.comment.error.CommentException;
import com.example.infinite.domain.artistcontent.comment.repository.CommentRepository;
import com.example.infinite.domain.artistcontent.comment.service.CommentMentionService;
import com.example.infinite.domain.artistcontent.comment.support.CommentReader;
import com.example.infinite.domain.artistcontent.comment.support.MentionParser;
import com.example.infinite.domain.artistcontent.post.eunms.PostType;
import com.example.infinite.domain.artistcontent.post.fanpost.repository.FanPostRepository;
import com.example.infinite.domain.artistcontent.post.fanpost.support.FanPostReader;
import com.example.infinite.domain.member.member.entity.Member;
import com.example.infinite.domain.member.member.support.MemberInputSupport;
import com.example.infinite.domain.member.member.support.MemberReader;
import com.example.infinite.domain.subscriptionmembership.dto.response.WriterSubscriptionBadge;
import com.example.infinite.domain.subscriptionmembership.service.SubscriptionMembershipService;
import com.example.infinite.global.auth.MemberDetailsImpl;
import lombok.RequiredArgsConstructor;
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

    /**
     * FanPost 댓글은 기존 동기 계약을 유지하되, count 갱신은 atomic update로 바꾼다.
     * 락은 reply 생성/삭제처럼 thread 충돌이 있는 지점에서만 바깥 서비스가 담당한다.
     */
    @Transactional
    public CommentResponse create(MemberDetailsImpl memberDetails, Long artistId, Long fanPostId, CommentCreateRequest request) {
        Member member = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));
        fanPostReader.findByIdAndArtistIdOrThrow(fanPostId, artistId);

        Comment parentComment = resolveParentComment(request.parentId(), fanPostId);
        String mentionNickname = resolveMentionNickname(request.content(), fanPostId, parentComment);

        Comment comment = commentRepository.save(Comment.create(
                PostType.FAN_POST,
                fanPostId,
                member,
                request.content(),
                parentComment
        ));
        fanPostRepository.changeCommentCountBy(fanPostId, 1L);

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
            return;
        }

        commentMentionService.deleteMention(comment.getId());

        if (comment.isRootComment()) {
            if (!commentRepository.existsByParentId(comment.getId())) {
                comment.delete();
            } else {
                comment.markDeletedPlaceholder();
            }
            fanPostRepository.changeCommentCountBy(fanPostId, -1L);
            return;
        }

        comment.delete();
        fanPostRepository.changeCommentCountBy(fanPostId, -1L);

        Comment parentComment = comment.getParent();
        if (parentComment != null && parentComment.isDeletedPlaceholder() && !commentRepository.existsByParentId(parentComment.getId())) {
            parentComment.delete();
        }
    }

    public Long resolveRootCommentId(Long fanPostId, Long commentId) {
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

        Comment parentComment = commentReader.findByIdAndTargetTypeAndTargetIdOrThrow(parentId, PostType.FAN_POST, fanPostId);
        if (!parentComment.isRootComment()) {
            throw new CommentException(CommentErrorCode.COMMENT_DEPTH_EXCEEDED);
        }
        return parentComment;
    }

    private String resolveMentionNickname(String content, Long fanPostId, Comment parentComment) {
        List<String> mentionedNicknames = MentionParser.extractMentionedNicknames(content);
        if (mentionedNicknames.isEmpty() || parentComment == null) {
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

        return mentionedNicknames.stream()
                .filter(allowedNicknames::contains)
                .findFirst()
                .orElse(null);
    }

    private WriterSubscriptionBadge loadSingleWriterBadge(Long artistId, Long writerId) {
        return subscriptionMembershipService.getWriterBadges(artistId, List.of(writerId))
                .getOrDefault(writerId, WriterSubscriptionBadge.empty(writerId));
    }
}
