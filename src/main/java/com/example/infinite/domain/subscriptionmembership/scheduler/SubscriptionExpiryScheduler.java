package com.example.infinite.domain.subscriptionmembership.scheduler;

import com.example.infinite.domain.subscriptionmembership.enums.SubscriptionStatus;
import com.example.infinite.domain.subscriptionmembership.repository.DmSubscriptionRepository;
import com.example.infinite.domain.subscriptionmembership.repository.FanMembershipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionExpiryScheduler {

    private final DmSubscriptionRepository dmSubscriptionRepository;
    private final FanMembershipRepository fanMembershipRepository;

    @Scheduled(cron = "0 0 * * * *")  // 매시 정각 실행
    @Transactional
    public void expireDmSubscriptions() {
        var expired = dmSubscriptionRepository
                .findByStatusAndExpiredAtBefore(SubscriptionStatus.ACTIVE, LocalDateTime.now());
        expired.forEach(sub -> sub.expire());
        log.info("DM 구독 만료 처리 완료: {}건", expired.size());
    }

    @Scheduled(cron = "0 0 * * * *")  // 매시 정각 실행
    @Transactional
    public void expireFanMemberships() {
        var expired = fanMembershipRepository
                .findByStatusAndExpiredAtBefore(SubscriptionStatus.ACTIVE, LocalDateTime.now());
        expired.forEach(membership -> membership.expire());
        log.info("팬 멤버십 만료 처리 완료: {}건", expired.size());
    }
}
