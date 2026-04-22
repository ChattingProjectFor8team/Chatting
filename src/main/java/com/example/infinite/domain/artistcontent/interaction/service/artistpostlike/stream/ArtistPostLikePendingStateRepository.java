package com.example.infinite.domain.artistcontent.interaction.service.artistpostlike.stream;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ArtistPostLikePendingStateRepository {

    private static final Duration PENDING_STATE_TTL = Duration.ofMinutes(30);
    private static final DefaultRedisScript<Long> CLEAR_IF_VERSION_MATCH_SCRIPT;

    static {
        CLEAR_IF_VERSION_MATCH_SCRIPT = new DefaultRedisScript<>();
        CLEAR_IF_VERSION_MATCH_SCRIPT.setScriptText("""
                if redis.call('get', KEYS[2]) == ARGV[1] then
                    redis.call('del', KEYS[1])
                    return 1
                end
                return 0
                """);
        CLEAR_IF_VERSION_MATCH_SCRIPT.setResultType(Long.class);
    }

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * V3 요청 경로에서 아직 DB에 반영되지 않은 "유저의 최신 의도 상태"를 보관한다.
     *
     * 왜 필요한가:
     * - V3는 좋아요를 stream에 넣고 나중에 consumer가 DB 반영한다
     * - 따라서 같은 유저가 빠르게 두 번 누르면 DB 상태만 보고는 현재 토글 의도를 알 수 없다
     * - pending state를 Redis에 두면 "소비되기 전 최신 상태"를 요청 경로에서 참고할 수 있다
     */
    public Optional<Boolean> findPendingDesiredState(Long artistPostId, Long memberId) {
        String state = stringRedisTemplate.opsForValue().get(buildStateKey(artistPostId, memberId));
        if (state == null) {
            return Optional.empty();
        }
        return Optional.of(Boolean.parseBoolean(state));
    }

    public long nextPendingVersion(Long artistPostId, Long memberId) {
        // version은 "이 pending state가 몇 번째 요청까지 반영한 상태인지"를 구분하는 용도다.
        // consumer가 오래된 명령을 처리할 때 더 최신 pending state를 잘못 지우지 않게 한다.
        String versionKey = buildVersionKey(artistPostId, memberId);
        Long nextVersion = stringRedisTemplate.opsForValue().increment(versionKey);
        stringRedisTemplate.expire(versionKey, PENDING_STATE_TTL);
        return nextVersion == null ? 1L : nextVersion;
    }

    public void savePendingDesiredState(Long artistPostId, Long memberId, boolean desiredReacted) {
        stringRedisTemplate.opsForValue().set(
                buildStateKey(artistPostId, memberId),
                Boolean.toString(desiredReacted),
                PENDING_STATE_TTL
        );
    }

    public void clearPendingStateIfVersionMatches(Long artistPostId, Long memberId, long version) {
        // 최신 요청이 이미 한 번 더 들어온 상태라면 version이 달라지므로 old consumer가 pending state를 지우지 못한다.
        stringRedisTemplate.execute(
                CLEAR_IF_VERSION_MATCH_SCRIPT,
                List.of(buildStateKey(artistPostId, memberId), buildVersionKey(artistPostId, memberId)),
                Long.toString(version)
        );
    }

    private String buildStateKey(Long artistPostId, Long memberId) {
        return "artist-post:like:pending:state:" + artistPostId + ":" + memberId;
    }

    private String buildVersionKey(Long artistPostId, Long memberId) {
        return "artist-post:like:pending:version:" + artistPostId + ":" + memberId;
    }
}
