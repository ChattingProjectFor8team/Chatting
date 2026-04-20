package com.example.infinite.domain.artistcontent.post.fanpost.repository;

import com.example.infinite.domain.artistcontent.post.fanpost.entity.FanPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FanPostRepository extends JpaRepository<FanPost, Long>, FanPostRepositoryCustom {
    Optional<FanPost> findByIdAndArtistId(Long fanPostId, Long artistId);
}
