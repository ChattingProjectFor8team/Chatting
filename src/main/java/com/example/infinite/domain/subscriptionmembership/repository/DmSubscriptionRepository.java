package com.example.infinite.domain.subscriptionmembership.repository;

import com.example.infinite.domain.subscriptionmembership.entity.DmSubscription;
import com.example.infinite.domain.subscriptionmembership.enums.SubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DmSubscriptionRepository extends JpaRepository<DmSubscription, Long> {

    Optional<DmSubscription> findByUserIdAndArtistIdAndStatus(Long userId, Long artistId, SubscriptionStatus status);

    Page<DmSubscription> findByUserId(Long userId, Pageable pageable);

    List<DmSubscription> findByStatusAndExpiredAtBefore(SubscriptionStatus status, LocalDateTime now);

    // 특정 아티스트에 대해 여러 유저의 DM 구독 상태를 한 번에 조회 (뱃지 배치 조회용, N+1 방지)
    List<DmSubscription> findByArtistIdAndUserIdInAndStatus(Long artistId, Collection<Long> userIds, SubscriptionStatus status);

    // 환불 요청 시 본인 소유 구독인지 검증하며 단건 조회
    Optional<DmSubscription> findByIdAndUserId(Long id, Long userId);
}
