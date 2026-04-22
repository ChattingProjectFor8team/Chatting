package com.example.infinite.domain.artistcontent.comment.repository;

import com.example.infinite.domain.artistcontent.comment.entity.Comment;
import com.example.infinite.domain.artistcontent.post.enums.PostType;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface CommentRepositoryCustom {

    List<Comment> findRootSliceByTargetTypeAndTargetId(PostType targetType, Long targetId, Long cursor, int limit);

    List<Comment> findRepliesByParentId(Long parentId, int limit);

    Map<Long, Integer> findReplyCountsByParentIds(Collection<Long> parentIds);

    List<Comment> findThreadCommentsByRootCommentId(PostType targetType, Long targetId, Long rootCommentId);
}
