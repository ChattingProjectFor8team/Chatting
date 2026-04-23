package com.example.infinite.domain.artistcontent.media.dto.response;

import com.example.infinite.domain.artistcontent.media.entity.ArtistYoutubeVideo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "아티스트 유튜브 영상 카드 응답")
public record ArtistYoutubeVideoResponse(
        Long id,
        Long artistId,
        Long writerMemberId,
        String writerDisplayName,
        String writerProfileImageUrl,
        String youtubeVideoId,
        String youtubeUrl,
        String title,
        String thumbnailUrl,
        long durationSeconds,
        LocalDateTime publishedAt,
        LocalDateTime createdAt
) {
    public static ArtistYoutubeVideoResponse from(ArtistYoutubeVideo entity) {
        return new ArtistYoutubeVideoResponse(
                entity.getId(),
                entity.getArtistId(),
                entity.getWriterMemberId(),
                entity.getWriterDisplayName(),
                entity.getWriterProfileImageUrl(),
                entity.getYoutubeVideoId(),
                entity.getYoutubeUrl(),
                entity.getTitle(),
                entity.getThumbnailUrl(),
                entity.getDurationSeconds(),
                entity.getPublishedAt(),
                entity.getCreatedAt()
        );
    }
}
