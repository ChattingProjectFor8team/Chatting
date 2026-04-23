package com.example.infinite.domain.artistcontent.post.artistpost.dto.response;

import com.example.infinite.domain.artistcontent.post.cache.PostHotData;

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
    public static ArtistPostResponse from(ArtistPostBaseResponse baseResponse, PostHotData hotData) {
        // base/hot 분리 캐시를 쓴 뒤, 응답 직전에만 하나로 합친다.
        return new ArtistPostResponse(
                baseResponse.artistPostId(),
                baseResponse.artistId(),
                baseResponse.writerId(),
                baseResponse.writerNickname(),
                baseResponse.writerProfileImageUrl(),
                baseResponse.artistBadge(),
                baseResponse.content(),
                hotData.likeCount(),
                hotData.commentCount(),
                baseResponse.mediaCount(),
                baseResponse.media(),
                baseResponse.hashtags(),
                baseResponse.createdAt()
        );
    }
}
