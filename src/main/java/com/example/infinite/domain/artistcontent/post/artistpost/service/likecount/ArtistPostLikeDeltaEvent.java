package com.example.infinite.domain.artistcontent.post.artistpost.service.likecount;

public record ArtistPostLikeDeltaEvent(
        Long artistPostId,
        long delta
) {
    // 트랜잭션 커밋 이후 Redis delta 누적으로 넘기기 위한 경량 이벤트다.
    // 핵심은 "원본 DB 저장은 지금, count 반영은 나중"을 분리하는 데 있다.
}
