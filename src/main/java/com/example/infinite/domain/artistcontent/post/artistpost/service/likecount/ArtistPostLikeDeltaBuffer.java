package com.example.infinite.domain.artistcontent.post.artistpost.service.likecount;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ArtistPostLikeDeltaBuffer {

    private static final String ARTIST_POST_LIKE_DELTA_KEY = "artist-post:like:delta";

    /**
     * hash field를 읽고 지우는 작업을 한 번에 처리한다.
     * flush 도중 새 delta가 들어와도 유실하지 않고 다음 주기에 넘기기 위한 atomic drain 용도다.
     */
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
     * Redis hash를 "변경량 버퍼"로 사용한다.
     *
     * 예:
     * - artistPostId=10 글에 좋아요 3번, 취소 1번이 들어오면
     * - hash field "10" 값은 최종적으로 +2만 남는다
     *
     * 이 방식의 장점:
     * - DB에 요청마다 update 하지 않아도 된다
     * - 같은 글에 대한 짧은 시간 내 다수 요청을 한 값으로 압축할 수 있다
     */
    public void accumulate(Long artistPostId, long delta) {
        stringRedisTemplate.opsForHash().increment(
                ARTIST_POST_LIKE_DELTA_KEY,
                artistPostId.toString(),
                delta
        );
    }

    /**
     * 현재 Redis에 쌓인 모든 post별 delta를 읽어 간다.
     *
     * 주의:
     * - 단순 HGET 후 HDEL 두 번으로 처리하면 flush 도중 delta 유실 가능성이 있다
     * - 그래서 field 단위 drain은 Lua로 원자 처리한다
     */
    public List<ArtistPostLikeDelta> drainAll() {
        Set<Object> artistPostIds = stringRedisTemplate.opsForHash().keys(ARTIST_POST_LIKE_DELTA_KEY);
        if (artistPostIds == null || artistPostIds.isEmpty()) {
            return List.of();
        }

        List<ArtistPostLikeDelta> drainedDeltas = new ArrayList<>();
        for (Object artistPostIdField : artistPostIds) {
            Long artistPostId = Long.valueOf(artistPostIdField.toString());
            long delta = drainOne(artistPostId);
            if (delta != 0L) {
                drainedDeltas.add(new ArtistPostLikeDelta(artistPostId, delta));
            }
        }
        return drainedDeltas;
    }

    /**
     * flush 실패 시 이번 배치가 들고 있던 delta를 Redis로 복구한다.
     *
     * Redis drain과 DB update는 하나의 원자 트랜잭션이 아니므로,
     * DB 쪽이 실패하면 Redis에서 뺀 값을 직접 다시 적재해야 유실을 막을 수 있다.
     */
    public void restoreAll(List<ArtistPostLikeDelta> deltas) {
        if (deltas == null || deltas.isEmpty()) {
            return;
        }

        for (ArtistPostLikeDelta delta : deltas) {
            accumulate(delta.artistPostId(), delta.delta());
        }
    }

    private long drainOne(Long artistPostId) {
        // "읽고 삭제"를 한 번에 수행해야 flush와 동시 쓰기가 겹칠 때 값이 꼬이지 않는다.
        Long delta = stringRedisTemplate.execute(
                DRAIN_HASH_FIELD_SCRIPT,
                List.of(ARTIST_POST_LIKE_DELTA_KEY),
                artistPostId.toString()
        );
        return delta == null ? 0L : delta;
    }
}
