package com.example.infinite.domain.artistcontent.hashtag.repository;

import com.example.infinite.domain.artistcontent.hashtag.entity.Hashtag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HashtagRepository extends JpaRepository<Hashtag, Long> {

    Optional<Hashtag> findByName(String name);

    boolean existsByName(String name);

    List<Hashtag> findByNameStartingWithOrderByUsageCountDescNameAsc(String prefix, Pageable pageable);

    List<Hashtag> findByNameContainingOrderByUsageCountDescNameAsc(String keyword, Pageable pageable);

    List<Hashtag> findAllByOrderByUsageCountDescNameAsc(Pageable pageable);
}
