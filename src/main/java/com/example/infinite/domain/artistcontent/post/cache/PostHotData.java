package com.example.infinite.domain.artistcontent.post.cache;

/**
 * 좋아요/댓글처럼 자주 바뀌는 "hot 필드"만 따로 들고 다니는 값 객체다.
 *
 * base/hot 분리의 핵심 의도:
 * - 본문/미디어/해시태그는 긴 TTL
 * - count는 짧은 TTL
 * 로 서로 다른 수명을 갖게 만드는 것이다.
 */
public record PostHotData(
        long likeCount,
        long commentCount
) {
    public static PostHotData of(Long likeCount, Long commentCount) {
        return new PostHotData(
                likeCount == null ? 0L : likeCount,
                commentCount == null ? 0L : commentCount
        );
    }

    public static PostHotData likeOnly(Long likeCount) {
        return new PostHotData(likeCount == null ? 0L : likeCount, 0L);
    }

    public static PostHotData empty() {
        return new PostHotData(0L, 0L);
    }
}
