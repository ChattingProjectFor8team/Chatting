package com.example.infinite.domain.member.artist.repository;

import com.example.infinite.domain.member.artist.entity.Artist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArtistRepository extends JpaRepository<Artist, Long>, ArtistRepositoryCustom {
    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    Optional<Artist> findBySlug(String slug);
}
