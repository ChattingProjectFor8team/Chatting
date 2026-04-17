package com.example.infinite.domain.member.artist.repository;

import com.example.infinite.domain.member.artist.entity.Artist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistRepository extends JpaRepository<Artist, Long>, ArtistRepositoryCustom {
}
