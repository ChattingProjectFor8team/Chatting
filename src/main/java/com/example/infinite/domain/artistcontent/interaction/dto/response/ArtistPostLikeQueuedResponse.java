package com.example.infinite.domain.artistcontent.interaction.dto.response;

import java.time.LocalDateTime;

public record ArtistPostLikeQueuedResponse(
        String requestId,
        Long artistPostId,
        boolean expectedReacted,
        LocalDateTime queuedAt
) {
    // queued 응답은 "지금 DB에 반영된 최종 상태"가 아니라 "요청자가 의도한 다음 상태"를 알려준다.
}
