package com.example.infinite.domain.ArtistContent.Media.Repository;

import com.example.infinite.domain.ArtistContent.Media.Entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaRepository extends JpaRepository<Media, Long> {
}
