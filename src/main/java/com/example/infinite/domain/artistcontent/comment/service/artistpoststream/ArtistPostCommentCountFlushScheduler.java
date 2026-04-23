package com.example.infinite.domain.artistcontent.comment.service.artistpoststream;

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
public class ArtistPostCommentCountFlushScheduler {

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

        for (ArtistPostCommentDelta delta : deltas) {
            // commentCount는 요청 경로 대신 flush 배치가 주기적으로 반영한다.
            artistPostRepository.changeCommentCountBy(delta.artistPostId(), delta.delta());
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
}
