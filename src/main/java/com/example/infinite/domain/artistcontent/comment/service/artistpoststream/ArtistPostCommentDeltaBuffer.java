package com.example.infinite.domain.artistcontent.comment.service.artistpoststream;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * ArtistPost commentCount 의 "임시 증감치"를 Redis Hash 로 모아두는 버퍼다.
 *
 * 왜 필요한가:
 * - 댓글 생성/삭제마다 DB count 컬럼을 바로 갱신하면 hot post 에 쓰기 경쟁이 커진다
 * - 먼저 Redis 에 delta 만 쌓아두고
 * - scheduler 가 모아서 DB에 반영하면 write pressure 를 줄일 수 있다
 */
@Component
@RequiredArgsConstructor
public class ArtistPostCommentDeltaBuffer {

    private static final String ARTIST_POST_COMMENT_DELTA_KEY = "artist-post:comment:delta";
    private static final DefaultRedisScript<Long> DRAIN_HASH_FIELD_SCRIPT;

    static {
        DRAIN_HASH_FIELD_SCRIPT = new DefaultRedisScript<>();
        DRAIN_HASH_FIELD_SCRIPT.setScriptText("""
                local current = redis.call('HGET', KEYS[1], ARGV[1])
                if not current then
                    return 0
                end
                redis.call('HDEL', KEYS[1], ARGV[1])
                return tonumber(current)
                """);
        DRAIN_HASH_FIELD_SCRIPT.setResultType(Long.class);
    }

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 댓글 1개 생성/삭제가 끝날 때마다 delta 를 누적한다.
     * 같은 게시글로 burst 가 와도 Redis hash field 하나에 계속 합산된다.
     */
    public void accumulate(Long artistPostId, long delta) {
        stringRedisTemplate.opsForHash().increment(
                ARTIST_POST_COMMENT_DELTA_KEY,
                artistPostId.toString(),
                delta
        );
    }

    /**
     * scheduler 가 호출하는 drain 메서드다.
     *
     * drain 의 의미:
     * - 읽기만 하는 것이 아니라
     * - Redis 에 쌓인 값을 가져오면서 비워
     * - "이번 flush 배치가 책임질 delta"로 확정하는 과정
     */
    public List<ArtistPostCommentDelta> drainAll() {
        Set<Object> artistPostIds = stringRedisTemplate.opsForHash().keys(ARTIST_POST_COMMENT_DELTA_KEY);
        if (artistPostIds == null || artistPostIds.isEmpty()) {
            return List.of();
        }

        List<ArtistPostCommentDelta> deltas = new ArrayList<>();
        for (Object artistPostIdField : artistPostIds) {
            Long artistPostId = Long.valueOf(artistPostIdField.toString());
            long delta = drainOne(artistPostId);
            if (delta != 0L) {
                deltas.add(new ArtistPostCommentDelta(artistPostId, delta));
            }
        }
        return deltas;
    }

    private long drainOne(Long artistPostId) {
        // HGET 후 HDEL 을 따로 하면 중간 경쟁에서 중복 반영 위험이 생길 수 있으므로
        // Lua script 로 "읽고 바로 삭제"를 원자적으로 처리한다.
        Long delta = stringRedisTemplate.execute(
                DRAIN_HASH_FIELD_SCRIPT,
                List.of(ARTIST_POST_COMMENT_DELTA_KEY),
                artistPostId.toString()
        );
        return delta == null ? 0L : delta;
    }
}
