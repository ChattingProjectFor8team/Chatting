package com.example.infinite.domain.artistcontent.post.fanpost.dto.response;

import com.example.infinite.domain.artistcontent.post.cache.PostHotData;

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
    public static FanPostResponse from(FanPostBaseResponse baseResponse, PostHotData hotData) {
        // base 응답과 hot count를 마지막에 합쳐 최종 API 응답을 만든다.
        return new FanPostResponse(
                baseResponse.fanPostId(),
                baseResponse.artistId(),
                baseResponse.writerId(),
                baseResponse.writerNickname(),
                baseResponse.writerProfileImageUrl(),
                baseResponse.fanMembershipSubscribed(),
                baseResponse.dmSubscribed(),
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
