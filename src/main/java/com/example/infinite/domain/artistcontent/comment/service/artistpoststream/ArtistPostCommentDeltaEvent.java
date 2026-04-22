package com.example.infinite.domain.artistcontent.comment.service.artistpoststream;

public record ArtistPostCommentDeltaEvent(
        Long artistPostId,
        long delta
) {
}
