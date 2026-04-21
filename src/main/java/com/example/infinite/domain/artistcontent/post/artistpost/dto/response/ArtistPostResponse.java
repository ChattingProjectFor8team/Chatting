package com.example.infinite.domain.artistcontent.post.artistpost.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record ArtistPostResponse(
        Long artistPostId,
        Long artistId,
        Long writerId,
        String writerNickname,
        String writerProfileImageUrl,
        boolean artistBadge,
        String content,
        long likeCount,
        long commentCount,
        int mediaCount,
        List<ArtistPostMediaResponse> media,
        List<String> hashtags,
        LocalDateTime createdAt
) {
    public static ArtistPostResponse from(
            ArtistPostReadRow row,
            List<ArtistPostMediaResponse> media,
            List<String> hashtags
    ) {
        return new ArtistPostResponse(
                row.artistPostId(),
                row.artistId(),
                row.writerId(),
                row.writerNickname(),
                row.writerProfileImageUrl(),
                Boolean.TRUE.equals(row.artistBadge()),
                row.content(),
                row.likeCount() == null ? 0L : row.likeCount(),
                row.commentCount() == null ? 0L : row.commentCount(),
                row.mediaCount() == null ? 0 : row.mediaCount(),
                media,
                hashtags,
                row.createdAt()
        );
    }
}
