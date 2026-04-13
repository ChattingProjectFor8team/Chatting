package com.example.infinite.domain.ArtistContent.Repository;

import com.example.infinite.domain.ArtistContent.Entity.ArtistContent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistContentRepository extends JpaRepository<ArtistContent, Long>, ArtistContentRepositoryCustom {
}
