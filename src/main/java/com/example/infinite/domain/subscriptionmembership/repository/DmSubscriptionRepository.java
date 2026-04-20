package com.example.infinite.domain.subscriptionmembership.repository;

import com.example.infinite.domain.subscriptionmembership.entity.DmSubscription;
import com.example.infinite.domain.subscriptionmembership.enums.SubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DmSubscriptionRepository extends JpaRepository<DmSubscription, Long> {

    Optional<DmSubscription> findByUserIdAndArtistIdAndStatus(Long userId, Long artistId, SubscriptionStatus status);

    Page<DmSubscription> findByUserId(Long userId, Pageable pageable);

    List<DmSubscription> findByStatusAndExpiredAtBefore(SubscriptionStatus status, LocalDateTime now);
}
