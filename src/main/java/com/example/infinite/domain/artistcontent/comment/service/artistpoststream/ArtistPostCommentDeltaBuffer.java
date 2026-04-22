package com.example.infinite.domain.artistcontent.comment.service.artistpoststream;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

    public void accumulate(Long artistPostId, long delta) {
        stringRedisTemplate.opsForHash().increment(
                ARTIST_POST_COMMENT_DELTA_KEY,
                artistPostId.toString(),
                delta
        );
    }

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
        Long delta = stringRedisTemplate.execute(
                DRAIN_HASH_FIELD_SCRIPT,
                List.of(ARTIST_POST_COMMENT_DELTA_KEY),
                artistPostId.toString()
        );
        return delta == null ? 0L : delta;
    }
}
