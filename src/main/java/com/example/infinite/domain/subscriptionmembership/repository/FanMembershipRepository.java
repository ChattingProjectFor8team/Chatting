package com.example.infinite.domain.subscriptionmembership.repository;

import com.example.infinite.domain.subscriptionmembership.entity.FanMembership;
import com.example.infinite.domain.subscriptionmembership.enums.SubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FanMembershipRepository extends JpaRepository<FanMembership, Long> {

    Optional<FanMembership> findByUserIdAndArtistIdAndStatus(Long userId, Long artistId, SubscriptionStatus status);

    Page<FanMembership> findByUserId(Long userId, Pageable pageable);
}
