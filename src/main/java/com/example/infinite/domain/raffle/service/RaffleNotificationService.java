package com.example.infinite.domain.raffle.service;

import com.example.infinite.domain.raffle.dto.RaffleNotification;
import com.example.infinite.domain.raffle.entity.Raffle;
import com.example.infinite.domain.raffle.entity.RaffleSlotWinner;
import com.example.infinite.domain.raffle.enums.RewardType;
import com.example.infinite.domain.raffle.repository.RaffleEntryRepository;
import com.example.infinite.domain.raffle.repository.RaffleSlotWinnerRepository;
import com.example.infinite.domain.subscriptionmembership.enums.SubscriptionStatus;
import com.example.infinite.domain.subscriptionmembership.repository.DmSubscriptionRepository;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RaffleNotificationService {

    private static final int MEMBERSHIP_EXTENSION_DAYS = 30;
    private static final int JITTER_MAX_SECONDS = 5;

    private final SimpMessagingTemplate messagingTemplate;
    private final DmSubscriptionRepository dmSubscriptionRepository;
    private final RaffleEntryRepository raffleEntryRepository;
    private final RaffleSlotWinnerRepository raffleSlotWinnerRepository;

    private final ScheduledExecutorService jitterExecutor = Executors.newScheduledThreadPool(1);

    /**
     * 당첨자에게 알림 전송 + DM 구독 연장 (MEMBERSHIP_EXTENSION 자동 이행).
     * - reward_type이 MEMBERSHIP_EXTENSION이면: 구독 +30일 연장 + rewardStatus GRANTED
     * - STOMP Push로 당첨 알림
     */
    @Transactional
    public void handleWinner(Raffle raffle, RaffleSlotWinner winner) {
        if (raffle.getRewardType() == RewardType.MEMBERSHIP_EXTENSION) {
            extendDmSubscription(winner.getUserId(), raffle.getArtistId());
            winner.grant();
            log.info("DM 구독 연장 완료: userId={}, artistId={}, +{}일",
                    winner.getUserId(), raffle.getArtistId(), MEMBERSHIP_EXTENSION_DAYS);
        }

        sendNotification(
                winner.getUserId(),
                RaffleNotification.win(raffle.getId(), raffle.getTitle())
        );
    }

    /**
     * 래플 완료 시 비당첨자에게 일괄 알림.
     * - Jitter: 0~5초 랜덤 지연으로 STOMP 서버 부하 분산
     * - 비동기: 래플 완료 트랜잭션을 블로킹하지 않음
     */
    public void notifyLosers(Raffle raffle) {
        List<Long> allEntryUserIds = raffleEntryRepository.findUserIdsByRaffleId(raffle.getId());

        Set<Long> winnerUserIds = raffleSlotWinnerRepository.findByRaffleId(raffle.getId())
                .stream()
                .map(RaffleSlotWinner::getUserId)
                .collect(Collectors.toSet());

        List<Long> loserUserIds = allEntryUserIds.stream()
                .filter(userId -> !winnerUserIds.contains(userId))
                .toList();

        if (loserUserIds.isEmpty()) {
            return;
        }

        RaffleNotification notification = RaffleNotification.lose(raffle.getId(), raffle.getTitle());

        for (Long userId : loserUserIds) {
            long delaySec = ThreadLocalRandom.current().nextLong(0, JITTER_MAX_SECONDS + 1);
            jitterExecutor.schedule(
                    () -> sendNotification(userId, notification),
                    delaySec,
                    TimeUnit.SECONDS
            );
        }

        log.info("비당첨자 알림 예약: raffleId={}, losers={}, jitter=0~{}s",
                raffle.getId(), loserUserIds.size(), JITTER_MAX_SECONDS);
    }

    @PreDestroy
    public void shutdown() {
        jitterExecutor.shutdown();
    }

    private void sendNotification(Long userId, RaffleNotification notification) {
        try {
            messagingTemplate.convertAndSend(
                    "/sub/user/" + userId + "/notifications",
                    notification
            );
        } catch (Exception e) {
            log.warn("래플 알림 전송 실패: userId={}, type={}, error={}",
                    userId, notification.type(), e.getMessage());
        }
    }

    private void extendDmSubscription(Long userId, Long artistId) {
        dmSubscriptionRepository
                .findByUserIdAndArtistIdAndStatus(userId, artistId, SubscriptionStatus.ACTIVE)
                .ifPresentOrElse(
                        subscription -> subscription.extendExpiry(MEMBERSHIP_EXTENSION_DAYS),
                        () -> log.warn("활성 DM 구독 없음 — 연장 불가: userId={}, artistId={}", userId, artistId)
                );
    }
}
