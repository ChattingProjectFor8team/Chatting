package com.example.infinite.domain.ArtistContent.Post.ArtistPost.Repository;

import com.example.infinite.domain.ArtistContent.Post.ArtistPost.Entity.ArtistPost;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistPostRepository extends JpaRepository<ArtistPost, Long>, ArtistPostRepositoryCustom {
}
