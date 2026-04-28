package com.example.infinite.domain.raffle.service;

import com.example.infinite.domain.raffle.dto.EntryResponse;
import com.example.infinite.domain.raffle.enums.EntryCondition;
import com.example.infinite.domain.raffle.error.RaffleErrorCode;
import com.example.infinite.domain.raffle.error.RaffleException;
import com.example.infinite.domain.subscriptionmembership.entity.DmSubscription;
import com.example.infinite.domain.subscriptionmembership.enums.SubscriptionStatus;
import com.example.infinite.domain.subscriptionmembership.repository.DmSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RaffleEntryService {

    private final ReservoirSampler reservoirSampler;
    private final DmSubscriptionRepository dmSubscriptionRepository;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String META_KEY_FORMAT = "raffle:%d:meta";

    /**
     * 응모 처리. DB 호출은 구독 검증 시 최대 1회, 나머지는 전부 Redis.
     */
    public EntryResponse enter(Long artistId, Long raffleId, Long userId) {
        // 1. Redis 메타 조회
        String metaKey = String.format(META_KEY_FORMAT, raffleId);
        Map<Object, Object> meta = stringRedisTemplate.opsForHash().entries(metaKey);

        if (meta.isEmpty()) {
            throw new RaffleException(RaffleErrorCode.RAFFLE_NOT_FOUND);
        }

        // 2. 상태 검증
        if (!"ACTIVE".equals(meta.get("status"))) {
            throw new RaffleException(RaffleErrorCode.RAFFLE_NOT_ACTIVE);
        }

        // 3. artistId 소속 검증
        if (!String.valueOf(artistId).equals(meta.get("artistId"))) {
            throw new RaffleException(RaffleErrorCode.RAFFLE_ARTIST_MISMATCH);
        }

        // 4. 구독 검증 (MEMBERSHIP_ONLY인 경우 — 유일한 DB 호출)
        EntryCondition condition = EntryCondition.valueOf((String) meta.get("entryCondition"));
        if (condition == EntryCondition.MEMBERSHIP_ONLY) {
            boolean hasActiveSubscription = dmSubscriptionRepository
                    .findByUserIdAndArtistIdAndStatus(userId, artistId, SubscriptionStatus.ACTIVE)
                    .map(DmSubscription::isActive)
                    .orElse(false);

            if (!hasActiveSubscription) {
                throw new RaffleException(RaffleErrorCode.RAFFLE_MEMBERSHIP_REQUIRED);
            }
        }

        // 5. 현재 슬롯 인덱스 계산
        LocalDateTime startedAt = LocalDateTime.parse((String) meta.get("startedAt"));
        long slotDurationSec = Long.parseLong((String) meta.get("slotDurationSec"));
        int totalSlots = Integer.parseInt((String) meta.get("totalSlots"));
        int currentSlotIndex = (int) (Duration.between(startedAt, LocalDateTime.now()).getSeconds() / slotDurationSec);

        if (currentSlotIndex >= totalSlots) {
            throw new RaffleException(RaffleErrorCode.RAFFLE_NOT_ACTIVE);
        }

        // 6. Reservoir Sampling 실행 (중복 체크 + Lua Script + 감사 로그 포함)
        ReservoirSampler.EntryResult result = reservoirSampler.enter(raffleId, currentSlotIndex, userId);

        if (!result.success()) {
            switch (result.reason()) {
                case "ALREADY_ENTERED" -> throw new RaffleException(RaffleErrorCode.RAFFLE_ALREADY_ENTERED);
                case "SLOT_CLOSED" -> throw new RaffleException(RaffleErrorCode.RAFFLE_SLOT_CLOSED);
                default -> throw new RaffleException(RaffleErrorCode.RAFFLE_ENTRY_FAILED);
            }
        }

        log.info("응모 성공: raffleId={}, userId={}, slot={}, order={}",
                raffleId, userId, currentSlotIndex, result.entryOrder());
        return EntryResponse.success();
    }
}