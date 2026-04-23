package com.example.infinite.domain.artistcontent.follow.repository;

import com.example.infinite.domain.artistcontent.follow.entity.Follow;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    Optional<Follow> findByFollowerMemberIdAndTargetArtistMemberId(Long followerMemberId, Long targetArtistMemberId);

    @EntityGraph(attributePaths = {
            "targetArtistMember",
            "targetArtistMember.artist",
            "targetArtistMember.member"
    })
    List<Follow> findAllByFollowerMemberIdOrderByIdDesc(Long followerMemberId);
}
