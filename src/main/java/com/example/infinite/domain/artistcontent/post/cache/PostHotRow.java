package com.example.infinite.domain.artistcontent.post.cache;

/**
 * DB에서 hot 필드만 읽어올 때 사용하는 공통 projection row다.
 *
 * id + count만 가져오므로
 * 본문/미디어 같은 무거운 read model을 다시 만들지 않고도
 * 짧은 TTL 캐시를 채울 수 있다.
 */
public record PostHotRow(
        Long postId,
        Long likeCount,
        Long commentCount
) {
    /**
     * commentCount가 없는 도메인(FanLetter)용 보조 생성자.
     * JPQL에서 상수 0L를 직접 넣는 대신 이 생성자를 쓰면 IDE 파서 경고를 줄일 수 있다.
     */
    public PostHotRow(Long postId, Long likeCount) {
        this(postId, likeCount, 0L);
    }
}
