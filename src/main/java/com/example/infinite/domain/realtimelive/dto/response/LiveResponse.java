package com.example.infinite.domain.realtimelive.dto.response;

import com.example.infinite.domain.realtimelive.entity.RealtimeLive;
import com.example.infinite.domain.realtimelive.enums.LiveStatus;

import java.time.LocalDateTime;

public record LiveResponse(
        Long id,
        Long artistId,
        String title,
        String description,
        LiveStatus liveStatus,
        String thumbnailUrl,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        String replayUrl,
        LocalDateTime createdAt
) {
    public static LiveResponse from(RealtimeLive entity) {
        return new LiveResponse(
                entity.getId(),
                entity.getArtistId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getLiveStatus(),
                entity.getThumbnailUrl(),
                entity.getStartedAt(),
                entity.getEndedAt(),
                entity.getReplayUrl(),
                entity.getCreatedAt()
        );
    }
}
