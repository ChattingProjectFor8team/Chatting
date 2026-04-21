package com.example.infinite.domain.artistcontent.post.fanpost.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record FanPostResponse(
        Long fanPostId,
        Long artistId,
        Long writerId,
        String writerNickname,
        String writerProfileImageUrl,
        boolean fanMembershipSubscribed,
        boolean dmSubscribed,
        String content,
        long likeCount,
        long commentCount,
        int mediaCount,
        List<FanPostMediaResponse> media,
        List<String> hashtags,
        LocalDateTime createdAt
) {
    public static FanPostResponse from(FanPostReadRow row, List<FanPostMediaResponse> media, List<String> hashtags) {
        // projection row와 media 목록을 최종 API 응답 구조로 합친다.
        return new FanPostResponse(
                row.fanPostId(),
                row.artistId(),
                row.writerId(),
                row.writerNickname(),
                row.writerProfileImageUrl(),
                Boolean.TRUE.equals(row.fanMembershipSubscribed()),
                Boolean.TRUE.equals(row.dmSubscribed()),
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
