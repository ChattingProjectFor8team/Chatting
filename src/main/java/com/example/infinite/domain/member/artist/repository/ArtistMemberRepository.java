package com.example.infinite.domain.member.artist.repository;

import com.example.infinite.domain.member.artist.entity.ArtistMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArtistMemberRepository extends JpaRepository<ArtistMember, Long> {
    boolean existsByMemberId(Long memberId);

    boolean existsByArtistIdAndMemberId(Long artistId, Long memberId);

    Optional<ArtistMember> findByIdAndArtistId(Long id, Long artistId);

    Optional<ArtistMember> findByArtistIdAndMemberId(Long artistId, Long memberId);

    long countByArtistId(Long artistId);

    List<ArtistMember> findAllByArtistId(Long artistId);
}
