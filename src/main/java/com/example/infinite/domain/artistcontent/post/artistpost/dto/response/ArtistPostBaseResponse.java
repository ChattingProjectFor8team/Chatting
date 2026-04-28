package com.example.infinite.domain.artistcontent.post.artistpost.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * artist post 조회 응답에서 변동이 적은 base 필드만 담는 DTO다.
 *
 * count를 따로 떼어내면
 * flush 주기와 무관한 본문/미디어는 더 길게 캐시할 수 있다.
 */
public record ArtistPostBaseResponse(
        Long artistPostId,
        Long artistId,
        Long writerId,
        String writerNickname,
        String writerProfileImageUrl,
        boolean artistBadge,
        String content,
        int mediaCount,
        List<ArtistPostMediaResponse> media,
        List<String> hashtags,
        LocalDateTime createdAt
) {
    public static ArtistPostBaseResponse from(
            ArtistPostReadRow row,
            List<ArtistPostMediaResponse> media,
            List<String> hashtags
    ) {
        return new ArtistPostBaseResponse(
                row.artistPostId(),
                row.artistId(),
                row.writerId(),
                row.writerNickname(),
                row.writerProfileImageUrl(),
                Boolean.TRUE.equals(row.artistBadge()),
                row.content(),
                row.mediaCount() == null ? 0 : row.mediaCount(),
                media,
                hashtags,
                row.createdAt()
        );
    }
}
