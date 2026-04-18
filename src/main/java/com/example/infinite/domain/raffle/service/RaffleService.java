package com.example.infinite.domain.raffle.service;

import com.example.infinite.domain.raffle.dto.CreateRaffleRequest;
import com.example.infinite.domain.raffle.dto.RaffleResponse;
import com.example.infinite.domain.raffle.entity.Raffle;
import com.example.infinite.domain.raffle.entity.RaffleSlot;
import com.example.infinite.domain.raffle.entity.RaffleSlotWinner;
import com.example.infinite.domain.raffle.enums.RaffleStatus;
import com.example.infinite.domain.raffle.enums.RewardType;
import com.example.infinite.domain.raffle.error.RaffleErrorCode;
import com.example.infinite.domain.raffle.error.RaffleException;
import com.example.infinite.domain.raffle.repository.RaffleRepository;
import com.example.infinite.domain.raffle.repository.RaffleSlotRepository;
import com.example.infinite.domain.raffle.repository.RaffleSlotWinnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RaffleService {

    private final RaffleRepository raffleRepository;
    private final RaffleSlotRepository raffleSlotRepository;
    private final RaffleSlotWinnerRepository raffleSlotWinnerRepository;
    private final ReservoirSampler reservoirSampler;
    private final RaffleSchedulerService schedulerService;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String META_KEY_FORMAT = "raffle:%d:meta";
    private static final Duration META_TTL_AFTER_COMPLETE = Duration.ofMinutes(5);

    // ─── Admin: 래플 생성 ───

    @Transactional
    public RaffleResponse createRaffle(Long artistId, CreateRaffleRequest request) {
        if (request.rewardType() != RewardType.MEMBERSHIP_EXTENSION) {
            throw new RaffleException(RaffleErrorCode.RAFFLE_INVALID_REWARD_TYPE);
        }

        Raffle raffle = Raffle.builder()
                .artistId(artistId)
                .title(request.title())
                .entryCondition(request.entryCondition())
                .rewardType(request.rewardType())
                .totalWinners(request.totalWinners())
                .durationMinutes(request.durationMinutes())
                .build();
        raffleRepository.save(raffle);

        for (int i = 0; i < request.totalWinners(); i++) {
            RaffleSlot slot = RaffleSlot.builder()
                    .raffle(raffle)
                    .slotIndex(i)
                    .targetWinnerCount(1)
                    .build();
            raffleSlotRepository.save(slot);
        }

        log.info("래플 생성: id={}, artistId={}, slots={}", raffle.getId(), artistId, request.totalWinners());
        return RaffleResponse.from(raffle);
    }

    // ─── Admin: 래플 시작 ───

    @Transactional
    public RaffleResponse startRaffle(Long artistId, Long raffleId) {
        Raffle raffle = findRaffleWithArtistCheck(artistId, raffleId);

        if (raffle.getStatus() != RaffleStatus.PENDING) {
            throw new RaffleException(RaffleErrorCode.RAFFLE_NOT_PENDING);
        }

        raffle.start();

        List<RaffleSlot> slots = raffleSlotRepository.findByRaffleOrderBySlotIndex(raffle);
        long slotDurationSec = (long) raffle.getDurationMinutes() * 60 / slots.size();
        LocalDateTime cursor = raffle.getStartedAt();

        for (RaffleSlot slot : slots) {
            slot.fillTimes(cursor, cursor.plusSeconds(slotDurationSec));
            cursor = cursor.plusSeconds(slotDurationSec);
        }
        slots.get(0).activate();

        initRedisMeta(raffle, slotDurationSec);
        schedulerService.scheduleSlotCloses(raffle, slots);

        log.info("래플 시작: id={}, slots={}, slotDuration={}s", raffleId, slots.size(), slotDurationSec);
        return RaffleResponse.from(raffle);
    }

    // ─── Admin: 래플 취소 ───

    @Transactional
    public RaffleResponse cancelRaffle(Long artistId, Long raffleId) {
        Raffle raffle = findRaffleWithArtistCheck(artistId, raffleId);

        if (raffle.getStatus() == RaffleStatus.COMPLETED || raffle.getStatus() == RaffleStatus.CANCELED) {
            throw new RaffleException(RaffleErrorCode.RAFFLE_CANCEL_NOT_ALLOWED);
        }

        boolean wasActive = raffle.getStatus() == RaffleStatus.ACTIVE;
        raffle.cancel();

        if (wasActive) {
            schedulerService.cancelScheduledTasks(raffleId);
            cleanupRedisKeys(raffleId);
        }

        log.info("래플 취소: id={}, wasActive={}", raffleId, wasActive);
        return RaffleResponse.from(raffle);
    }

    // ─── 슬롯 종료 (스케줄러에서 호출) ───

    @Transactional
    public void closeSlot(Long raffleId, int slotIndex) {
        Raffle raffle = raffleRepository.findById(raffleId)
                .orElseThrow(() -> new RaffleException(RaffleErrorCode.RAFFLE_NOT_FOUND));

        if (raffle.getStatus() != RaffleStatus.ACTIVE) {
            log.warn("래플이 ACTIVE가 아닌 상태에서 슬롯 종료 시도: id={}, status={}", raffleId, raffle.getStatus());
            return;
        }

        List<RaffleSlot> slots = raffleSlotRepository.findByRaffleOrderBySlotIndex(raffle);
        RaffleSlot currentSlot = slots.get(slotIndex);

        ReservoirSampler.SlotCloseResult result = reservoirSampler.closeSlot(raffleId, slotIndex);

        if (!result.isEmpty()) {
            Long winnerId = Long.parseLong(result.candidateUserId());
            RaffleSlotWinner winner = RaffleSlotWinner.builder()
                    .raffle(raffle)
                    .slot(currentSlot)
                    .userId(winnerId)
                    .build();
            raffleSlotWinnerRepository.save(winner);
            currentSlot.complete();
            log.info("슬롯 당첨자 확정: raffleId={}, slot={}, winner={}", raffleId, slotIndex, winnerId);
        } else {
            currentSlot.markEmpty(currentSlot.getTargetWinnerCount());
            log.info("슬롯 빈 종료: raffleId={}, slot={}, carryOver={}", raffleId, slotIndex, currentSlot.getTargetWinnerCount());

            // carry-over: 다음 슬롯에 이월 (DB 기록용, k>1 Lua는 Phase 2 이후)
            int nextIndex = slotIndex + 1;
            if (nextIndex < slots.size()) {
                slots.get(nextIndex).addCarryOver(currentSlot.getTargetWinnerCount());
            }
        }

        // 다음 슬롯 활성화 또는 래플 완료
        int nextIndex = slotIndex + 1;
        if (nextIndex < slots.size()) {
            slots.get(nextIndex).activate();
        } else {
            completeRaffle(raffle);
        }
    }

    // ─── 래플 완료 ───

    private void completeRaffle(Raffle raffle) {
        raffle.complete();

        // Redis 키 TTL 5분 설정 (지연 요청 방어 후 자연 소멸)
        String metaKey = String.format(META_KEY_FORMAT, raffle.getId());
        stringRedisTemplate.expire(metaKey, META_TTL_AFTER_COMPLETE);

        // Reservoir Sampling 운영 키에도 TTL 설정
        List<RaffleSlot> slots = raffleSlotRepository.findByRaffleOrderBySlotIndex(raffle);
        for (RaffleSlot slot : slots) {
            reservoirSampler.setSlotKeysTtl(raffle.getId(), slot.getSlotIndex(), META_TTL_AFTER_COMPLETE);
        }

        log.info("래플 완료: id={}", raffle.getId());
    }

    // ─── Redis 메타 초기화 ───

    private void initRedisMeta(Raffle raffle, long slotDurationSec) {
        String metaKey = String.format(META_KEY_FORMAT, raffle.getId());
        Map<String, String> meta = Map.of(
                "status", "ACTIVE",
                "artistId", String.valueOf(raffle.getArtistId()),
                "entryCondition", raffle.getEntryCondition().name(),
                "totalSlots", String.valueOf(raffle.getTotalWinners()),
                "startedAt", raffle.getStartedAt().toString(),
                "slotDurationSec", String.valueOf(slotDurationSec)
        );
        stringRedisTemplate.opsForHash().putAll(metaKey, meta);
    }

    // ─── Redis 키 정리 (취소 시) ───

    private void cleanupRedisKeys(Long raffleId) {
        String metaKey = String.format(META_KEY_FORMAT, raffleId);
        stringRedisTemplate.delete(metaKey);
        // 슬롯별 키는 ReservoirSampler.cleanupSlotKeys()로 개별 정리 가능하나,
        // TTL이 보험으로 걸려있으므로 meta만 즉시 삭제
    }

    // ─── 공통 조회 + artistId 검증 ───

    public Raffle findRaffleWithArtistCheck(Long artistId, Long raffleId) {
        Raffle raffle = raffleRepository.findById(raffleId)
                .orElseThrow(() -> new RaffleException(RaffleErrorCode.RAFFLE_NOT_FOUND));

        if (!raffle.getArtistId().equals(artistId)) {
            throw new RaffleException(RaffleErrorCode.RAFFLE_ARTIST_MISMATCH);
        }
        return raffle;
    }
}