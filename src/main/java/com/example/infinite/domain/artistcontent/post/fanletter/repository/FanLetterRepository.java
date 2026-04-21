package com.example.infinite.domain.artistcontent.post.fanletter.repository;

import com.example.infinite.domain.artistcontent.post.fanletter.entity.FanLetter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FanLetterRepository extends JpaRepository<FanLetter, Long>, FanLetterRepositoryCustom {

    // 팬레터 상세/수정/삭제는 항상 artist 범위 안에서 조회해야
    // 다른 아티스트의 팬레터를 잘못 건드리지 않는다.
    Optional<FanLetter> findByIdAndArtistId(Long fanLetterId, Long artistId);
}
