package com.example.infinite.domain.artistcontent.post.fanpost.dto.response;

import java.time.LocalDateTime;

// HOT 목록은 카드 조립용 기본 필드 외에
// 복합커서 계산에 필요한 hotScore도 함께 읽어 와야 한다.
public record FanPostHotReadRow(
        Long fanPostId,
        Long artistId,
        Long writerId,
        String writerNickname,
        String writerProfileImageUrl,
        Boolean fanMembershipSubscribed,
        Boolean dmSubscribed,
        String content,
        Integer mediaCount,
        LocalDateTime createdAt,
        Long hotScore
) {
    public FanPostReadRow toBaseReadRow() {
        return new FanPostReadRow(
                fanPostId,
                artistId,
                writerId,
                writerNickname,
                writerProfileImageUrl,
                fanMembershipSubscribed,
                dmSubscribed,
                content,
                mediaCount,
                createdAt
        );
    }
}
