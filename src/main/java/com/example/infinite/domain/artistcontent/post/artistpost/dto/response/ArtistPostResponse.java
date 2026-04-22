package com.example.infinite.domain.artistcontent.post.artistpost.dto.response;

import java.time.LocalDateTime;
import java.util.List;

// ArtistPost 목록 카드와 상세 본문이 공통으로 쓰는 기본 응답 구조다.
// 작성자 표시는 stageName이 아니라 실제 로그인 Member 닉네임 기준이다.
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
        // QueryDSL projection row + media + hashtag를 최종 API 응답 구조로 조립한다.
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
