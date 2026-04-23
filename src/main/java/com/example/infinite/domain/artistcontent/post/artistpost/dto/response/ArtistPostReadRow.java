package com.example.infinite.domain.artistcontent.post.artistpost.dto.response;

import java.time.LocalDateTime;

// 목록/상세 공통 projection row다.
// 작성자는 ArtistMember의 stageName이 아니라 실제 로그인 Member의 닉네임으로 노출한다.
public record ArtistPostReadRow(
        Long artistPostId,
        Long artistId,
        Long writerId,
        String writerNickname,
        String writerProfileImageUrl,
        Boolean artistBadge,
        String content,
        Integer mediaCount,
        LocalDateTime createdAt
) {
}
