package com.example.infinite.global.common.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisStreamGroupHelper {

    /**
     * Redis Stream과 consumer group 초기화를 안전하게 보장하는 헬퍼다.
     *
     * 학습 포인트:
     * - Stream consumer는 group이 없으면 XREADGROUP에서 NOGROUP 에러가 난다.
     * - 그래서 producer/initializer가 "stream 존재 + group 존재"를 먼저 맞춰 줘야 한다.
     */

    private final StringRedisTemplate stringRedisTemplate;
    private final Set<String> initializedGroups = ConcurrentHashMap.newKeySet();

    /**
     * stream + consumer group 조합을 1회만 초기화한다.
     * 첫 요청보다 consumer group 생성이 늦어지면 이벤트를 놓칠 수 있어 producer 진입 전에 보장한다.
     */
    public void ensureGroup(String streamKey, String groupName) {
        String cacheKey = streamKey + "::" + groupName;
        if (initializedGroups.contains(cacheKey)) {
            return;
        }

        synchronized (this) {
            // 여러 스레드가 동시에 첫 초기화를 시도할 수 있어 JVM 내부에서는 한 번만 통과시킨다.
            if (initializedGroups.contains(cacheKey)) {
                return;
            }

            try {
                bootstrapStreamIfMissing(streamKey);
                stringRedisTemplate.opsForStream().createGroup(streamKey, ReadOffset.latest(), groupName);
                log.info("Redis Stream group created: streamKey={}, group={}", streamKey, groupName);
            } catch (Exception e) {
                String message = e.getMessage();
                if (message != null && message.contains("BUSYGROUP")) {
                    log.debug("Redis Stream group already exists: streamKey={}, group={}", streamKey, groupName);
                } else {
                    throw e;
                }
            }

            initializedGroups.add(cacheKey);
        }
    }

    private void bootstrapStreamIfMissing(String streamKey) {
        // Redis Stream group는 stream이 존재하지 않으면 생성할 수 없다.
        // 그래서 첫 요청 전에 bootstrap record를 하나 넣어 group 초기화 실패를 피한다.
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(streamKey))) {
            return;
        }

        stringRedisTemplate.opsForStream().add(StreamRecords.newRecord()
                .in(streamKey)
                .ofMap(Map.of(
                        "__bootstrap__", "true",
                        "timestamp", Instant.now().toString()
                )));
    }
}
