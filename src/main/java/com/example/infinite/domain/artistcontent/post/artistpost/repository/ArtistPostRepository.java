package com.example.infinite.domain.artistcontent.post.artistpost.repository;

import com.example.infinite.domain.artistcontent.post.artistpost.entity.ArtistPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArtistPostRepository extends JpaRepository<ArtistPost, Long>, ArtistPostRepositoryCustom {

    Optional<ArtistPost> findByIdAndArtistId(Long artistPostId, Long artistId);
}
