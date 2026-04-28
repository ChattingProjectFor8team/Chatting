package com.example.infinite.domain.artistcontent.post.artistpost.service.likecount;

import com.example.infinite.domain.artistcontent.post.artistpost.repository.ArtistPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArtistPostLikeCountFlushScheduler {

    private static final int LOG_SAMPLE_SIZE = 10;

    private final ArtistPostLikeDeltaBuffer artistPostLikeDeltaBuffer;
    private final ArtistPostRepository artistPostRepository;
    // consumer와 같은 이유다.
    // 트래픽 테스트는 flush 시점을 직접 제어해야 "이번 poll에서 어디까지 반영됐는지"를 볼 수 있다.
    // 자동 스케줄 flush가 뒤에서 같이 돌면 수동 테스트가 반쯤 benchmark처럼 변해 버리므로 끌 수 있게 둔다.
    @Value("${artist-post.scheduler.enabled:true}")
    private boolean schedulerEnabled;

    /**
     * 1차 배치: flush
     *
     * 역할:
     * - Redis에 누적된 "변경이 있었던 글"만 DB count에 반영
     * - 전체 Reaction을 다시 세지 않으므로 자주 돌아도 부담이 상대적으로 작다
     *
     * 핵심 의도:
     * - 요청 경로에서 DB count row hotspot을 제거
     * - 대신 짧은 주기의 배치로 화면 count를 따라가게 만들기
     *
     * 캐시와의 관계:
     * - post hot cache TTL도 현재 3초라서
     * - flush 주기와 읽기 캐시 수명이 거의 같은 eventual consistency 리듬을 만든다
     */
    @Scheduled(fixedDelayString = "${artist-post.like-count.flush-delay-ms:3000}")
    @Transactional
    public void flushOnSchedule() {
        if (!schedulerEnabled) {
            return;
        }
        flush();
    }

    @Transactional
    public void flush() {
        // 실제 flush 로직은 별도 메서드에 둔다.
        // 운영에서는 flushOnSchedule() -> flush()로 타고,
        // 테스트에서는 flush()를 직접 호출해 매 drain tick 뒤의 count 변화를 관찰한다.
        List<ArtistPostLikeDelta> deltas = artistPostLikeDeltaBuffer.drainAll();
        if (deltas.isEmpty()) {
            return;
        }

        try {
            for (ArtistPostLikeDelta delta : deltas) {
                // 인기 글은 요청 쓰기를 빠르게 받고, DB count는 scheduler가 주기적으로 몰아서 반영한다.
                int updatedRowCount = artistPostRepository.changeLikeCountBy(delta.artistPostId(), delta.delta());
                if (updatedRowCount == 0) {
                    log.debug("ArtistPost likeCount flush skipped: artistPostId={}, delta={}", delta.artistPostId(), delta.delta());
                }
            }
        } catch (RuntimeException e) {
            // DB 반영 실패 시 drained delta를 Redis에 다시 넣어야
            // 롤백 이후에도 다음 flush/reconcile 전까지 값이 증발하지 않는다.
            try {
                artistPostLikeDeltaBuffer.restoreAll(deltas);
            } catch (RuntimeException restoreException) {
                // 복구도 실패하면 이번 배치 delta를 자동으로 되살릴 수 없으므로,
                // 운영자가 수동 복구할 수 있게 실패한 대상과 delta를 로그로 남긴다.
                log.error(
                        "ArtistPost likeCount flush restore failed: deltaCount={}, sample={}",
                        deltas.size(),
                        summarizeDeltas(deltas),
                        restoreException
                );
                e.addSuppressed(restoreException);
            }
            throw e;
        }
    }

    /**
     * 2차 배치: full reconcile
     *
     * 역할:
     * - 하루 1회 Reaction 원본 기준으로 like_count를 전수 보정
     * - flush 누락, 운영 중 장애, 예외 케이스로 생길 수 있는 드리프트를 최종 수정
     *
     * flush와 reconcile의 차이:
     * - flush: 최근에 변한 글만
     * - reconcile: 전체 활성 ArtistPost를 다시 계산
     *
     * 즉 flush는 "실시간에 가까운 추적", reconcile은 "하루 1회 원본 기준 정산"이다.
     */
    @Scheduled(cron = "${artist-post.like-count.reconcile-cron:0 0 4 * * *}")
    @Transactional
    public void reconcileAll() {
        // 하루 1회 원본 Reaction 집계를 다시 세어 delta flush 누락이나 운영 중 드리프트를 최종 보정한다.
        int updatedRowCount = artistPostRepository.reconcileAllLikeCounts();
        log.info("ArtistPost likeCount full reconcile completed: updatedRows={}", updatedRowCount);
    }

    private String summarizeDeltas(List<ArtistPostLikeDelta> deltas) {
        return deltas.stream()
                .limit(LOG_SAMPLE_SIZE)
                .map(delta -> delta.artistPostId() + ":" + delta.delta())
                .collect(Collectors.joining(", ", "[", deltas.size() > LOG_SAMPLE_SIZE ? ", ...]" : "]"));
    }
}
