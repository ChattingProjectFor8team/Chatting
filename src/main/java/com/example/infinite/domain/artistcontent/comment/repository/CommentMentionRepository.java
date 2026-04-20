package com.example.infinite.domain.artistcontent.comment.repository;

import com.example.infinite.domain.artistcontent.comment.entity.CommentMention;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CommentMentionRepository extends JpaRepository<CommentMention, Long> {

    // 댓글 수정 시에는 기존 멘션을 전부 지우고 새 본문 기준으로 다시 동기화한다.
    void deleteAllByComment_Id(Long commentId);

    // 댓글 목록 응답 조립에서 N+1을 피하려고 멘션 대상 Member까지 한 번에 읽는다.
    @EntityGraph(attributePaths = "mentionedMember")
    List<CommentMention> findByComment_IdInOrderByComment_IdAscIdAsc(Collection<Long> commentIds);
}
