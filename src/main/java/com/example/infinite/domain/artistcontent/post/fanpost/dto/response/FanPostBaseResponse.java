package com.example.infinite.domain.artistcontent.post.fanpost.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * fan post 조회 응답에서 덜 변하는 base 필드만 모아둔 DTO다.
 *
 * 의도:
 * - 본문/작성자/미디어/해시태그는 긴 TTL 캐시
 * - like/comment count는 hot 캐시
 * 로 수명을 분리하기 위함이다.
 */
public record FanPostBaseResponse(
        Long fanPostId,
        Long artistId,
        Long writerId,
        String writerNickname,
        String writerProfileImageUrl,
        boolean fanMembershipSubscribed,
        boolean dmSubscribed,
        String content,
        int mediaCount,
        List<FanPostMediaResponse> media,
        List<String> hashtags,
        LocalDateTime createdAt
) {
    public static FanPostBaseResponse from(FanPostReadRow row, List<FanPostMediaResponse> media, List<String> hashtags) {
        return new FanPostBaseResponse(
                row.fanPostId(),
                row.artistId(),
                row.writerId(),
                row.writerNickname(),
                row.writerProfileImageUrl(),
                Boolean.TRUE.equals(row.fanMembershipSubscribed()),
                Boolean.TRUE.equals(row.dmSubscribed()),
                row.content(),
                row.mediaCount() == null ? 0 : row.mediaCount(),
                media,
                hashtags,
                row.createdAt()
        );
    }
}
