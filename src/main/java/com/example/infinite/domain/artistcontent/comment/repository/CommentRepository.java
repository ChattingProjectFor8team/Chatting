package com.example.infinite.domain.artistcontent.comment.repository;

import com.example.infinite.domain.artistcontent.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long>, CommentRepositoryCustom {
}
