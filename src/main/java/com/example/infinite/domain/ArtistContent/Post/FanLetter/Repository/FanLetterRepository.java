package com.example.infinite.domain.ArtistContent.Post.FanLetter.Repository;

import com.example.infinite.domain.ArtistContent.Post.FanLetter.Entity.FanLetter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FanLetterRepository extends JpaRepository<FanLetter, Long>, FanLetterRepositoryCustom {
}
