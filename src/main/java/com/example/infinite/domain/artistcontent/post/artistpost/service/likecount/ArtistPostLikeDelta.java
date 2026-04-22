package com.example.infinite.domain.artistcontent.post.artistpost.service.likecount;

public record ArtistPostLikeDelta(
        Long artistPostId,
        long delta
) {
    // flush 단계에서 "어느 글에 몇 개를 더하거나 뺄지"를 운반하는 값 객체다.
    // 원본 Reaction 전체를 들고 다니지 않고 postId + delta만 유지해 배치 비용을 낮춘다.
}
