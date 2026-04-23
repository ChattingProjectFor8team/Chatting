package com.example.infinite.domain.artistcontent.comment.service.artistpoststream;

import com.example.infinite.domain.artistcontent.post.artistpost.repository.ArtistPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArtistPostCommentCountFlushScheduler {

    private static final int LOG_SAMPLE_SIZE = 10;

    private final ArtistPostCommentDeltaBuffer artistPostCommentDeltaBuffer;
    private final ArtistPostRepository artistPostRepository;

    /**
     * 댓글 수 1차 반영 배치.
     * 변경된 post만 반영하므로 자주 돌아도 전체 재집계보다 부담이 훨씬 낮다.
     *
     * 읽기 경로에서는 commentCount를 hot cache로 짧게 캐시하므로,
     * flush 주기와 캐시 TTL을 같이 가져가면 숫자 체감이 부드럽게 맞춰진다.
     */
    @Scheduled(fixedDelayString = "${artist-post.comment-count.flush-delay-ms:3000}")
    @Transactional
    public void flush() {
        List<ArtistPostCommentDelta> deltas = artistPostCommentDeltaBuffer.drainAll();
        if (deltas.isEmpty()) {
            return;
        }

        try {
            for (ArtistPostCommentDelta delta : deltas) {
                // commentCount는 요청 경로 대신 flush 배치가 주기적으로 반영한다.
                artistPostRepository.changeCommentCountBy(delta.artistPostId(), delta.delta());
            }
        } catch (RuntimeException e) {
            // DB 반영이 실패하면 트랜잭션은 롤백되지만,
            // drainAll()로 이미 Redis에서 빠진 delta는 직접 되돌려야 다음 주기에 재시도할 수 있다.
            try {
                artistPostCommentDeltaBuffer.restoreAll(deltas);
            } catch (RuntimeException restoreException) {
                // 복구마저 실패하면 이번 배치 delta가 유실될 수 있으므로,
                // 운영자가 수동 복구할 수 있게 대상 post와 delta를 에러 로그에 남긴다.
                log.error(
                        "ArtistPost commentCount flush restore failed: deltaCount={}, sample={}",
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
     * 댓글 수 2차 보정 배치.
     * placeholder 규칙까지 포함한 실제 comments 원본 기준으로 comment_count를 다시 맞춘다.
     *
     * flush가 "최근 변화량 반영"이라면 reconcile은 "최종 원본 재정산" 역할이다.
     */
    @Scheduled(cron = "${artist-post.comment-count.reconcile-cron:0 10 4 * * *}")
    @Transactional
    public void reconcileAll() {
        int updatedRows = artistPostRepository.reconcileAllCommentCounts();
        log.info("ArtistPost commentCount full reconcile completed: updatedRows={}", updatedRows);
    }

    private String summarizeDeltas(List<ArtistPostCommentDelta> deltas) {
        return deltas.stream()
                .limit(LOG_SAMPLE_SIZE)
                .map(delta -> delta.artistPostId() + ":" + delta.delta())
                .collect(Collectors.joining(", ", "[", deltas.size() > LOG_SAMPLE_SIZE ? ", ...]" : "]"));
    }
}
