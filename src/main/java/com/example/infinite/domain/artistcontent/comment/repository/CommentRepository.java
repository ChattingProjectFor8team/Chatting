package com.example.infinite.domain.artistcontent.comment.repository;

import com.example.infinite.domain.artistcontent.comment.entity.Comment;
import com.example.infinite.domain.artistcontent.post.eunms.PostType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long>, CommentRepositoryCustom {

    Optional<Comment> findByIdAndTargetTypeAndTargetId(Long commentId, PostType targetType, Long targetId);

    List<Comment> findByParentIdOrderByIdAsc(Long parentId);
}
