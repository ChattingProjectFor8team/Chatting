package com.example.infinite.domain.subscriptionmembership.service;

import com.example.infinite.domain.payment.config.JellyProperties;
import com.example.infinite.domain.payment.enums.ReferenceType;
import com.example.infinite.domain.payment.service.JellyService;
import com.example.infinite.domain.subscriptionmembership.dto.response.FanLetterWritePermission;
import com.example.infinite.domain.subscriptionmembership.dto.response.SubscriptionHistoryResponse;
import com.example.infinite.domain.subscriptionmembership.dto.response.SubscriptionStatusResponse;
import com.example.infinite.domain.subscriptionmembership.dto.response.WriterSubscriptionBadge;
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
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    // ── 타 도메인 공개 API ────────────────────────────────────────────────────

    /**
     * DM 구독 활성 여부 확인.
     * 래플 응모(MEMBERSHIP_ONLY) 검증, DM 메시지 발송 시 구독 확인에 사용.
     * 다른 도메인이 DmSubscriptionRepository를 직접 주입받지 않아도 되도록 캡슐화.
     */
    @Transactional(readOnly = true)
    public boolean hasActiveSubscription(Long userId, Long artistId) {
        return dmSubscriptionRepository
                .findByUserIdAndArtistIdAndStatus(userId, artistId, SubscriptionStatus.ACTIVE)
                .map(DmSubscription::isActive)
                .orElse(false);
    }

    /**
     * 작성자 목록의 구독 뱃지 배치 조회.
     * 팬포스트·팬레터·댓글 목록에서 작성자 닉네임 옆 뱃지 표시에 사용.
     * IN절 단일 쿼리 2회(DM·멤버십)로 처리하여 N+1 없이 동작한다.
     *
     * @param artistId  뱃지 기준이 되는 아티스트
     * @param writerIds 뱃지를 표시할 작성자 userId 목록
     * @return key=writerId, value=뱃지 정보 (Map에 없는 writerId는 양쪽 모두 false)
     */
    @Transactional(readOnly = true)
    public Map<Long, WriterSubscriptionBadge> getWriterBadges(Long artistId, Collection<Long> writerIds) {
        if (writerIds == null || writerIds.isEmpty()) {
            return Map.of();
        }

        // DM 구독 중인 writerId Set
        Set<Long> dmSubscribedIds = dmSubscriptionRepository
                .findByArtistIdAndUserIdInAndStatus(artistId, writerIds, SubscriptionStatus.ACTIVE)
                .stream()
                .filter(DmSubscription::isActive)   // 만료일 이중 검증
                .map(DmSubscription::getUserId)
                .collect(Collectors.toSet());

        // 팬 멤버십 보유 중인 writerId Set
        Set<Long> fanMembershipIds = fanMembershipRepository
                .findByArtistIdAndUserIdInAndStatus(artistId, writerIds, SubscriptionStatus.ACTIVE)
                .stream()
                .filter(FanMembership::isActive)    // 만료일 이중 검증
                .map(FanMembership::getUserId)
                .collect(Collectors.toSet());

        return writerIds.stream()
                .distinct()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> new WriterSubscriptionBadge(
                                id,
                                fanMembershipIds.contains(id),
                                dmSubscribedIds.contains(id)
                        )
                ));
    }

    /**
     * 팬레터 작성 가능 여부 확인.
     * 팬레터는 팬 멤버십 보유자만 작성 가능하므로 FanMembership 활성 여부로 판단.
     *
     * @param artistId 대상 아티스트
     * @param memberId 팬레터를 쓰려는 유저 userId
     */
    @Transactional(readOnly = true)
    public FanLetterWritePermission checkFanLetterPermission(Long artistId, Long memberId) {
        boolean allowed = fanMembershipRepository
                .findByUserIdAndArtistIdAndStatus(memberId, artistId, SubscriptionStatus.ACTIVE)
                .map(FanMembership::isActive)
                .orElse(false);
        return new FanLetterWritePermission(artistId, memberId, allowed);
    }
}
