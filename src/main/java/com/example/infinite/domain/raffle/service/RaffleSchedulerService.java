package com.example.infinite.domain.raffle.service;

import com.example.infinite.domain.raffle.entity.Raffle;
import com.example.infinite.domain.raffle.entity.RaffleSlot;
import com.example.infinite.domain.raffle.enums.RaffleSlotStatus;
import com.example.infinite.domain.raffle.enums.RaffleStatus;
import com.example.infinite.domain.raffle.repository.RaffleRepository;
import com.example.infinite.domain.raffle.repository.RaffleSlotRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RaffleSchedulerService {

    private final RaffleService raffleService;
    private final RaffleRepository raffleRepository;
    private final RaffleSlotRepository raffleSlotRepository;

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    private final Map<Long, List<ScheduledFuture<?>>> scheduledTasks = new ConcurrentHashMap<>();

    // RaffleService ↔ RaffleSchedulerService 순환 참조 차단용 @Lazy 프록시 주입.
    public RaffleSchedulerService(@Lazy RaffleService raffleService,
                                  RaffleRepository raffleRepository,
                                  RaffleSlotRepository raffleSlotRepository) {
        this.raffleService = raffleService;
        this.raffleRepository = raffleRepository;
        this.raffleSlotRepository = raffleSlotRepository;
    }

    /**
     * 래플 시작 시 호출. 각 슬롯 종료 시각에 close 태스크를 예약한다.
     */
    public void scheduleSlotCloses(Raffle raffle, List<RaffleSlot> slots) {
        List<ScheduledFuture<?>> futures = new CopyOnWriteArrayList<>();

        for (RaffleSlot slot : slots) {
            long delaySec = Duration.between(LocalDateTime.now(), slot.getSlotEndAt()).getSeconds();
            if (delaySec < 0) delaySec = 0;

            ScheduledFuture<?> future = executor.schedule(
                    () -> executeSlotClose(raffle.getId(), slot.getSlotIndex()),
                    delaySec,
                    TimeUnit.SECONDS
            );
            futures.add(future);
            log.debug("슬롯 종료 예약: raffleId={}, slot={}, delay={}s", raffle.getId(), slot.getSlotIndex(), delaySec);
        }

        scheduledTasks.put(raffle.getId(), futures);
    }

    /**
     * 래플 취소 시 예약된 태스크를 모두 취소한다.
     */
    public void cancelScheduledTasks(Long raffleId) {
        List<ScheduledFuture<?>> futures = scheduledTasks.remove(raffleId);
        if (futures != null) {
            futures.forEach(f -> f.cancel(false));
            log.info("래플 스케줄 취소: id={}, tasks={}", raffleId, futures.size());
        }
    }

    /**
     * 서버 재시작 시 ACTIVE 래플의 남은 슬롯 태스크를 복구한다.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void recoverActiveRaffles() {
        List<Raffle> activeRaffles = raffleRepository.findByStatus(RaffleStatus.ACTIVE);

        for (Raffle raffle : activeRaffles) {
            List<RaffleSlot> slots = raffleSlotRepository.findByRaffleOrderBySlotIndex(raffle);
            LocalDateTime now = LocalDateTime.now();

            // 이미 종료 시각이 지난 슬롯은 즉시 실행, 아직 남은 슬롯은 예약
            List<ScheduledFuture<?>> futures = new CopyOnWriteArrayList<>();
            for (RaffleSlot slot : slots) {
                if (slot.getSlotEndAt() == null) continue;
                if (slot.getStatus() == RaffleSlotStatus.COMPLETED
                        || slot.getStatus() == RaffleSlotStatus.EMPTY) {
                    continue; // 이미 처리된 슬롯
                }

                long delaySec = Duration.between(now, slot.getSlotEndAt()).getSeconds();
                if (delaySec <= 0) {
                    // 이미 지난 슬롯 → 즉시 실행
                    executor.execute(() -> executeSlotClose(raffle.getId(), slot.getSlotIndex()));
                } else {
                    ScheduledFuture<?> future = executor.schedule(
                            () -> executeSlotClose(raffle.getId(), slot.getSlotIndex()),
                            delaySec,
                            TimeUnit.SECONDS
                    );
                    futures.add(future);
                }
            }
            scheduledTasks.put(raffle.getId(), futures);
            log.info("래플 복구: id={}, 재등록 슬롯={}", raffle.getId(), futures.size());
        }

        if (!activeRaffles.isEmpty()) {
            log.info("ACTIVE 래플 복구 완료: {}건", activeRaffles.size());
        }
    }

    private void executeSlotClose(Long raffleId, int slotIndex) {
        try {
            raffleService.closeSlot(raffleId, slotIndex);
        } catch (Exception e) {
            log.error("슬롯 종료 실패: raffleId={}, slot={}, error={}", raffleId, slotIndex, e.getMessage(), e);
        }
    }
}