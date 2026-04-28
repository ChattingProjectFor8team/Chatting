package com.example.infinite.domain.artistcontent.post.fanpost.dto.response;

import com.example.infinite.domain.artistcontent.post.fanpost.entity.FanPost;

import java.time.LocalDateTime;

public record FanPostCreateResponse(
        Long fanPostId,
        Long artistId,
        Long writerId,
        String content,
        LocalDateTime createdAt
) {
    public static FanPostCreateResponse from(FanPost fanPost) {
        // 생성 직후 응답은 이후 상세 응답의 최소 핵심 축만 먼저 내려준다.
        return new FanPostCreateResponse(
                fanPost.getId(),
                fanPost.getArtist().getId(),
                fanPost.getWriter().getId(),
                fanPost.getContent(),
                fanPost.getCreatedAt()
        );
    }
}
