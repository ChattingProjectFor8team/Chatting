package com.example.infinite.domain.artistcontent.comment.dto.response;

import com.example.infinite.domain.artistcontent.comment.entity.Comment;

import java.time.LocalDateTime;
import java.util.List;

public record CommentResponse(
        Long commentId,
        Long parentCommentId,
        int depth,
        Long writerId,
        String writerNickname,
        String writerProfileImageUrl,
        boolean fanMembershipSubscribed,
        boolean dmSubscribed,
        String content,
        long likeCount,
        // 현재 정책상 대댓글 멘션은 최대 1명만 해석한다.
        CommentMentionResponse mentionedMember,
        int replyCount,
        List<CommentResponse> replies,
        LocalDateTime createdAt
) {
    public static CommentResponse from(
            Comment comment,
            boolean fanMembershipSubscribed,
            boolean dmSubscribed,
            int replyCount,
            List<CommentResponse> replies,
            CommentMentionResponse mentionedMember
    ) {
        // 기본 댓글 조회는 부모만 내려주고, 대댓글은 별도 API에서 조회한다.
        return new CommentResponse(
                comment.getId(),
                comment.getParent() == null ? null : comment.getParent().getId(),
                comment.getDepth(),
                comment.getWriter().getId(),
                comment.getWriter().getNickname(),
                comment.getWriter().getProfileImageUrl(),
                fanMembershipSubscribed,
                dmSubscribed,
                comment.getContent(),
                comment.getLikeCount(),
                mentionedMember,
                replyCount,
                replies,
                comment.getCreatedAt()
        );
    }

    public static CommentResponse from(
            Comment comment,
            int replyCount,
            List<CommentResponse> replies,
            CommentMentionResponse mentionedMember
    ) {
        return from(comment, false, false, replyCount, replies, mentionedMember);
    }

    public static CommentResponse from(Comment comment, int replyCount, CommentMentionResponse mentionedMember) {
        return from(comment, replyCount, List.of(), mentionedMember);
    }

    public static CommentResponse from(Comment comment, CommentMentionResponse mentionedMember) {
        return from(comment, 0, List.of(), mentionedMember);
    }

    public static CommentResponse from(
            Comment comment,
            boolean fanMembershipSubscribed,
            boolean dmSubscribed,
            int replyCount,
            CommentMentionResponse mentionedMember
    ) {
        return from(comment, fanMembershipSubscribed, dmSubscribed, replyCount, List.of(), mentionedMember);
    }

    public static CommentResponse from(
            Comment comment,
            boolean fanMembershipSubscribed,
            boolean dmSubscribed,
            CommentMentionResponse mentionedMember
    ) {
        return from(comment, fanMembershipSubscribed, dmSubscribed, 0, List.of(), mentionedMember);
    }

    public static CommentResponse from(Comment comment) {
        return from(comment, 0, List.of(), null);
    }
}
