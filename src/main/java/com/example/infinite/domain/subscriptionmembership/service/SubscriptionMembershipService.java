package com.example.infinite.domain.subscriptionmembership.service;

import com.example.infinite.domain.payment.config.JellyProperties;
import com.example.infinite.domain.payment.enums.ReferenceType;
import com.example.infinite.domain.payment.service.JellyService;
import com.example.infinite.domain.subscriptionmembership.dto.response.SubscriptionHistoryResponse;
import com.example.infinite.domain.subscriptionmembership.dto.response.SubscriptionStatusResponse;
import com.example.infinite.domain.subscriptionmembership.entity.DmSubscription;
import com.example.infinite.domain.subscriptionmembership.entity.FanMembership;
import com.example.infinite.domain.subscriptionmembership.enums.SubscriptionStatus;
import com.example.infinite.domain.subscriptionmembership.repository.DmSubscriptionRepository;
import com.example.infinite.domain.subscriptionmembership.repository.FanMembershipRepository;
import com.example.infinite.global.common.dto.PageResponse;
import com.example.infinite.global.error.ErrorCode;
import com.example.infinite.global.error.SubscriptionMembershipException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SubscriptionMembershipService {

    private final DmSubscriptionRepository dmSubscriptionRepository;
    private final FanMembershipRepository fanMembershipRepository;
    private final JellyService jellyService;
    private final JellyProperties jellyProperties;

    @Transactional
    public void purchaseDmSubscription(Long userId, Long artistId) {
        dmSubscriptionRepository
                .findByUserIdAndArtistIdAndStatus(userId, artistId, SubscriptionStatus.ACTIVE)
                .ifPresent(s -> { throw new SubscriptionMembershipException(ErrorCode.SUB_ALREADY_EXISTS); });

        jellyService.use(userId, jellyProperties.dmSubscriptionCost(), ReferenceType.DM_SUB, null);

        LocalDateTime now = LocalDateTime.now();
        dmSubscriptionRepository.save(DmSubscription.builder()
                .userId(userId)
                .artistId(artistId)
                .startedAt(now)
                .expiredAt(now.plusDays(30))
                .jellyAmount(jellyProperties.dmSubscriptionCost())
                .build());
    }

    @Transactional
    public void purchaseFanMembership(Long userId, Long artistId) {
        fanMembershipRepository
                .findByUserIdAndArtistIdAndStatus(userId, artistId, SubscriptionStatus.ACTIVE)
                .ifPresent(s -> { throw new SubscriptionMembershipException(ErrorCode.SUB_ALREADY_EXISTS); });

        jellyService.use(userId, jellyProperties.fanMembershipCost(), ReferenceType.MEMBERSHIP, null);

        LocalDateTime now = LocalDateTime.now();
        fanMembershipRepository.save(FanMembership.builder()
                .userId(userId)
                .artistId(artistId)
                .startedAt(now)
                .expiredAt(now.plusDays(30))
                .jellyAmount(jellyProperties.fanMembershipCost())
                .build());
    }

    @Transactional(readOnly = true)
    public SubscriptionStatusResponse getDmSubscriptionStatus(Long userId, Long artistId) {
        return dmSubscriptionRepository
                .findByUserIdAndArtistIdAndStatus(userId, artistId, SubscriptionStatus.ACTIVE)
                .filter(DmSubscription::isActive)
                .map(s -> SubscriptionStatusResponse.of(true, s.getExpiredAt()))
                .orElse(SubscriptionStatusResponse.inactive());
    }

    @Transactional(readOnly = true)
    public SubscriptionStatusResponse getFanMembershipStatus(Long userId, Long artistId) {
        return fanMembershipRepository
                .findByUserIdAndArtistIdAndStatus(userId, artistId, SubscriptionStatus.ACTIVE)
                .filter(FanMembership::isActive)
                .map(s -> SubscriptionStatusResponse.of(true, s.getExpiredAt()))
                .orElse(SubscriptionStatusResponse.inactive());
    }

    @Transactional(readOnly = true)
    public PageResponse<SubscriptionHistoryResponse> getDmSubscriptionHistory(Long userId, Pageable pageable) {
        return new PageResponse<>(
                dmSubscriptionRepository.findByUserId(userId, pageable)
                        .map(SubscriptionHistoryResponse::from)
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<SubscriptionHistoryResponse> getFanMembershipHistory(Long userId, Pageable pageable) {
        return new PageResponse<>(
                fanMembershipRepository.findByUserId(userId, pageable)
                        .map(SubscriptionHistoryResponse::from)
        );
    }
}
