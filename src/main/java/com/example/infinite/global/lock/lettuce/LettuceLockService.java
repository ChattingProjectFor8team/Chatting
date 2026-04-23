package com.example.infinite.global.lock.lettuce;

import com.example.infinite.global.lock.LockException;
import com.example.infinite.global.lock.LockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component("lettuceLockService")
@RequiredArgsConstructor
public class LettuceLockService implements LockService {

    /**
     * Redis의 SET NX EX를 직접 사용해 분산 락을 구현한 버전이다.
     *
     * 학습 포인트:
     * - Redisson처럼 추상화된 락 객체를 쓰지 않고
     * - "키가 없을 때만 저장" + TTL + 소유자 UUID 검증을 직접 조합한다.
     *
     * 그래서 V1은 동작 원리를 이해하기 좋지만,
     * 재시도/해제/소유권 관리까지 모두 직접 책임져야 해 코드가 더 거칠다.
     */

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 스레드별 락 소유자 UUID 저장.
     * ThreadLocal로 격리하여 다른 스레드의 락을 해제하는 버그를 방지.
     */
    private final ThreadLocal<Map<String, String>> lockOwnerMap =
            ThreadLocal.withInitial(HashMap::new);

    /**
     * 스핀락 재시도 간격 (밀리초)
     */
    private static final long SPIN_LOCK_RETRY_INTERVAL_MS = 100;

    /**
     * Lua Script: key의 value가 내 UUID와 일치할 때만 삭제.
     * 원자적(atomic) 실행으로 다른 스레드의 락을 실수로 해제하는 것을 방지.
     */
    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "  return redis.call('del', KEYS[1]) " +
            "else " +
            "  return 0 " +
            "end";

    private static final DefaultRedisScript<Long> UNLOCK_REDIS_SCRIPT;

    static {
        UNLOCK_REDIS_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_REDIS_SCRIPT.setScriptText(UNLOCK_SCRIPT);
        UNLOCK_REDIS_SCRIPT.setResultType(Long.class);
    }

    @Override
    public void lock(String key, long waitTime, long leaseTime, TimeUnit timeUnit) {
        // 같은 키를 잡으려는 요청끼리만 경쟁시키고,
        // value에는 소유자 UUID를 저장해 "누가 락을 잡았는지"를 식별한다.
        String uuid = UUID.randomUUID().toString();
        long waitTimeMs = timeUnit.toMillis(waitTime);
        long leaseTimeSec = timeUnit.toSeconds(leaseTime);
        long deadline = System.currentTimeMillis() + waitTimeMs;

        while (System.currentTimeMillis() < deadline) {
            // SET NX EX 한 번으로 "없으면 저장 + TTL 부여"를 원자적으로 수행한다.
            Boolean acquired = stringRedisTemplate.opsForValue()
                    .setIfAbsent(key, uuid, Duration.ofSeconds(leaseTimeSec));

            if (Boolean.TRUE.equals(acquired)) {
                lockOwnerMap.get().put(key, uuid);
                log.debug("락 획득 성공 [Lettuce]: key={}, uuid={}", key, uuid);
                return;
            }

            try {
                // V1은 별도 큐 없이 짧게 잠들었다가 다시 시도하는 스핀락 패턴이다.
                Thread.sleep(SPIN_LOCK_RETRY_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new LockException("락 획득 중 인터럽트 발생 [Lettuce]: key=" + key);
            }
        }

        throw new LockException("락 획득 실패 (대기 시간 초과) [Lettuce]: key=" + key);
    }

    @Override
    public void unlock(String key) {
        String uuid = lockOwnerMap.get().remove(key);

        // 맵이 비었으면 메모리 누수 방지를 위해 ThreadLocal 정리
        if (lockOwnerMap.get().isEmpty()) {
            lockOwnerMap.remove();
        }

        if (uuid == null) {
            log.warn("락 해제 시도했으나 소유 정보 없음 [Lettuce]: key={}", key);
            return;
        }

        Long result = stringRedisTemplate.execute(
                UNLOCK_REDIS_SCRIPT,
                Collections.singletonList(key),
                uuid
        );

        if (result != null && result == 1L) {
            log.debug("락 해제 완료 [Lettuce]: key={}, uuid={}", key, uuid);
        } else {
            // TTL 만료 뒤 다른 요청이 같은 키를 재사용했을 수 있으므로,
            // 값 비교 없이 DEL 하면 남의 락을 지우는 심각한 버그가 된다.
            log.warn("락 해제 실패 (이미 만료 또는 소유자 불일치) [Lettuce]: key={}", key);
        }
    }
}
