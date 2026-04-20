package com.example.infinite.domain.artistcontent.post.fanpost.dto.response;

import java.time.LocalDateTime;

// 목록/상세 조회에서 조인 결과를 팬 게시글 응답으로 조립하기 전 받는 평탄 row다.
public record FanPostReadRow(
        Long fanPostId,
        Long artistId,
        Long writerId,
        String writerNickname,
        String writerProfileImageUrl,
        Boolean fanMembershipSubscribed,
        Boolean dmSubscribed,
        String content,
        Long likeCount,
        Long commentCount,
        Integer mediaCount,
        LocalDateTime createdAt
) {
}
