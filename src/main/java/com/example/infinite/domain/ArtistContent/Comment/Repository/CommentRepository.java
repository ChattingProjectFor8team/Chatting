package com.example.infinite.domain.ArtistContent.Comment.Repository;

import com.example.infinite.domain.ArtistContent.Comment.Entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long>, CommentRepositoryCustom {
}
