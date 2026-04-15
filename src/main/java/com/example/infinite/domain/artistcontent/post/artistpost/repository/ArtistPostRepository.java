package com.example.infinite.domain.artistcontent.post.artistpost.repository;

import com.example.infinite.domain.artistcontent.post.artistpost.entity.ArtistPost;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistPostRepository extends JpaRepository<ArtistPost, Long>, ArtistPostRepositoryCustom {
}
