package com.example.infinite.domain.ArtistContent.Post.FanPost.Repository;

import com.example.infinite.domain.ArtistContent.Post.FanPost.Entity.FanPost;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FanPostRepository extends JpaRepository<FanPost, Long>, FanPostRepositoryCustom {
}
