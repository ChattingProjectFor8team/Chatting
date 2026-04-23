package com.example.infinite.domain.artistcontent.comment.repository;

import com.example.infinite.domain.artistcontent.comment.entity.Comment;
import com.example.infinite.domain.artistcontent.post.enums.PostType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long>, CommentRepositoryCustom {

    Optional<Comment> findByIdAndTargetTypeAndTargetId(Long commentId, PostType targetType, Long targetId);

    Optional<Comment> findByCommandRequestId(String commandRequestId);

    List<Comment> findByParentIdOrderByIdAsc(Long parentId);

    long countByParentId(Long parentId);

    boolean existsByParentId(Long parentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Comment comment
               set comment.likeCount =
                   case
                       when comment.likeCount + :delta < 0 then 0
                       else comment.likeCount + :delta
                   end
             where comment.id = :commentId
            """)
    int changeLikeCountBy(@Param("commentId") Long commentId, @Param("delta") long delta);

    @Query("select comment.likeCount from Comment comment where comment.id = :commentId")
    Optional<Long> findLikeCountById(@Param("commentId") Long commentId);
}
