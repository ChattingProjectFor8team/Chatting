package com.example.infinite.domain.realtimelive.dto.response;

import com.example.infinite.domain.realtimelive.entity.RealtimeLive;

import java.time.Duration;
import java.time.LocalDateTime;

/*
 * 종료된 라이브 VOD 목록 카드 전용 응답이다.
 *
 * 라이브 상세용 LiveResponse 와 분리한 이유:
 * - 프론트가 바로 렌더링할 카드 필드만 내려준다.
 * - duration 은 엔티티 컬럼을 늘리지 않고 startedAt / endedAt 차이로 계산한다.
 */
public record LiveVodResponse(
        Long liveId,
        Long artistId,
        Long hostMemberId,
        String hostDisplayName,
        String hostProfileImageUrl,
        String title,
        String thumbnailUrl,
        String replayUrl,
        long durationSeconds,
        LocalDateTime replayPublishedAt
) {
    public static LiveVodResponse from(RealtimeLive entity) {
        return new LiveVodResponse(
                entity.getId(),
                entity.getArtistId(),
                entity.getHostMemberId(),
                entity.getHostDisplayName(),
                entity.getHostProfileImageUrl(),
                entity.getTitle(),
                entity.getThumbnailUrl(),
                entity.getReplayUrl(),
                calculateDurationSeconds(entity.getStartedAt(), entity.getEndedAt()),
                entity.getReplayPublishedAt()
        );
    }

    private static long calculateDurationSeconds(LocalDateTime startedAt, LocalDateTime endedAt) {
        if (startedAt == null || endedAt == null || endedAt.isBefore(startedAt)) {
            return 0L;
        }
        return Duration.between(startedAt, endedAt).getSeconds();
    }
}
