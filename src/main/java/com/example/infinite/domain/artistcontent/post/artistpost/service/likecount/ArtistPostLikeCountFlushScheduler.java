package com.example.infinite.domain.artistcontent.post.artistpost.service.likecount;

import com.example.infinite.domain.artistcontent.post.artistpost.repository.ArtistPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArtistPostLikeCountFlushScheduler {

    private final ArtistPostLikeDeltaBuffer artistPostLikeDeltaBuffer;
    private final ArtistPostRepository artistPostRepository;

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
    public void flush() {
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
            artistPostLikeDeltaBuffer.restoreAll(deltas);
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
}
