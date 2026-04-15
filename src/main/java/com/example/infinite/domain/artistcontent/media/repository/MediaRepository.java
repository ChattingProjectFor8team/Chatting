package com.example.infinite.domain.artistcontent.media.repository;

import com.example.infinite.domain.artistcontent.media.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaRepository extends JpaRepository<Media, Long> {
}
