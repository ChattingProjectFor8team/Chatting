package com.example.infinite.domain.Payment.Repository;

import com.example.infinite.domain.Payment.Entity.FanMembership;
import com.example.infinite.domain.Payment.Enums.SubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FanMembershipRepository extends JpaRepository<FanMembership, Long> {

    // 특정 아티스트에 대한 유저의 활성 멤버십 조회
    Optional<FanMembership> findByUserIdAndArtistIdAndStatus(Long userId, Long artistId, SubscriptionStatus status);

    // 유저의 멤버십 전체 이력 페이징 조회
    Page<FanMembership> findByUserId(Long userId, Pageable pageable);
}