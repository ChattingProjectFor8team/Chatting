package com.example.infinite.domain.artistcontent.post.fanletter.repository;

import com.example.infinite.domain.artistcontent.post.fanletter.entity.FanLetter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FanLetterRepository extends JpaRepository<FanLetter, Long>, FanLetterRepositoryCustom {
}
