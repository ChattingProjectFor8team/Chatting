package com.example.infinite.domain.artistcontent.post.artistpost.dto.response;

import com.example.infinite.domain.artistcontent.post.artistpost.entity.ArtistPost;

import java.time.LocalDateTime;

public record ArtistPostCreateResponse(
        Long artistPostId,
        Long artistId,
        Long writerId,
        String content,
        LocalDateTime createdAt
) {
    public static ArtistPostCreateResponse from(ArtistPost artistPost) {
        // 생성 직후에는 후속 상세 조회에 필요한 최소 키 축만 먼저 내려준다.
        return new ArtistPostCreateResponse(
                artistPost.getId(),
                artistPost.getArtist().getId(),
                artistPost.getWriter().getId(),
                artistPost.getContent(),
                artistPost.getCreatedAt()
        );
    }
}
