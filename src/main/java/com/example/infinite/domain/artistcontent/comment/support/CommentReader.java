package com.example.infinite.domain.artistcontent.comment.support;

import com.example.infinite.domain.artistcontent.comment.entity.Comment;
import com.example.infinite.domain.artistcontent.comment.error.CommentErrorCode;
import com.example.infinite.domain.artistcontent.comment.error.CommentException;
import com.example.infinite.domain.artistcontent.comment.repository.CommentRepository;
import com.example.infinite.domain.artistcontent.post.eunms.PostType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentReader {

    private final CommentRepository commentRepository;

    public Comment findByIdAndTargetTypeAndTargetIdOrThrow(Long commentId, PostType targetType, Long targetId) {
        // 댓글은 대상 콘텐츠 소속까지 함께 묶어서 찾아야 다른 게시글 댓글을 잘못 참조하지 않는다.
        return commentRepository.findByIdAndTargetTypeAndTargetId(commentId, targetType, targetId)
                .orElseThrow(() -> new CommentException(CommentErrorCode.COMMENT_NOT_FOUND));
    }
}
