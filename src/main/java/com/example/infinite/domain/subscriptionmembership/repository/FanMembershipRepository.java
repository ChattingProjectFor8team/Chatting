package com.example.infinite.domain.subscriptionmembership.repository;

import com.example.infinite.domain.subscriptionmembership.entity.FanMembership;
import com.example.infinite.domain.subscriptionmembership.enums.SubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FanMembershipRepository extends JpaRepository<FanMembership, Long> {

    Optional<FanMembership> findByUserIdAndArtistIdAndStatus(Long userId, Long artistId, SubscriptionStatus status);

    Page<FanMembership> findByUserId(Long userId, Pageable pageable);

    List<FanMembership> findByStatusAndExpiredAtBefore(SubscriptionStatus status, LocalDateTime now);

    // 특정 아티스트에 대해 여러 유저의 팬 멤버십 상태를 한 번에 조회 (뱃지 배치 조회용, N+1 방지)
    List<FanMembership> findByArtistIdAndUserIdInAndStatus(Long artistId, Collection<Long> userIds, SubscriptionStatus status);
}
