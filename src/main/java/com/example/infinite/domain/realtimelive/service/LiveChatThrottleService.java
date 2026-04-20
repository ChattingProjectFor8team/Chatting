package com.example.infinite.domain.realtimelive.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * 라이브 채팅 도배 방지 3중 검증.
 * 메시지 인입 시점에서 순서대로 체크하며, 위반 시 조용히 무시한다.
 *
 * 1. 뮤트 체크: Redis Set `live:{liveId}:muted`에 userId가 있으면 차단
 * 2. 쓰로틀링: Redis 슬라이딩 윈도우 — 사용자당 2건/초
 * 3. 글자수: 200자 초과 차단
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiveChatThrottleService {

    private static final int MAX_MESSAGES_PER_SECOND = 2;
    private static final int MAX_MESSAGE_LENGTH = 200;
    private static final Duration WINDOW_SIZE = Duration.ofSeconds(1);

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 메시지 전송 가능 여부를 판단한다.
     * @return true면 전송 허용, false면 차단 (조용히 무시)
     */
    public boolean isAllowed(Long liveId, Long userId, String message) {
        // 1. 뮤트 체크
        if (isMuted(liveId, userId)) {
            log.debug("뮤트된 사용자 메시지 차단: liveId={}, userId={}", liveId, userId);
            return false;
        }

        // 2. 쓰로틀링 체크 (슬라이딩 윈도우)
        if (!checkRateLimit(liveId, userId)) {
            log.debug("쓰로틀링 초과 메시지 차단: liveId={}, userId={}", liveId, userId);
            return false;
        }

        // 3. 글자수 체크
        if (message == null || message.length() > MAX_MESSAGE_LENGTH) {
            log.debug("글자수 초과/빈 메시지 차단: liveId={}, userId={}, length={}",
                    liveId, userId, message == null ? 0 : message.length());
            return false;
        }

        return true;
    }

    /**
     * 관리자가 특정 사용자를 뮤트한다.
     */
    public void mute(Long liveId, Long userId) {
        String key = muteKey(liveId);
        stringRedisTemplate.opsForSet().add(key, userId.toString());
    }

    /**
     * 관리자가 특정 사용자의 뮤트를 해제한다.
     */
    public void unmute(Long liveId, Long userId) {
        String key = muteKey(liveId);
        stringRedisTemplate.opsForSet().remove(key, userId.toString());
    }

    private boolean isMuted(Long liveId, Long userId) {
        String key = muteKey(liveId);
        return Boolean.TRUE.equals(
                stringRedisTemplate.opsForSet().isMember(key, userId.toString()));
    }

    /**
     * Redis Sorted Set 기반 슬라이딩 윈도우.
     * score = 현재 시각(epoch millis), member = 고유값(현재 시각 나노).
     * 1초 윈도우 내 멤버 수가 MAX_MESSAGES_PER_SECOND 이상이면 차단.
     */
    private boolean checkRateLimit(Long liveId, Long userId) {
        String key = throttleKey(liveId, userId);
        long now = Instant.now().toEpochMilli();
        long windowStart = now - WINDOW_SIZE.toMillis();

        // 윈도우 밖 오래된 항목 제거
        stringRedisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);

        // 현재 윈도우 내 요청 수 확인
        Long count = stringRedisTemplate.opsForZSet().zCard(key);
        if (count != null && count >= MAX_MESSAGES_PER_SECOND) {
            return false;
        }

        // 현재 요청 추가 (member를 nano 시각으로 고유하게)
        stringRedisTemplate.opsForZSet().add(key, String.valueOf(System.nanoTime()), now);

        // 키 TTL 설정 (윈도우 크기 + 여유 1초)
        stringRedisTemplate.expire(key, WINDOW_SIZE.plusSeconds(1));

        return true;
    }

    private String muteKey(Long liveId) {
        return "live:" + liveId + ":muted";
    }

    private String throttleKey(Long liveId, Long userId) {
        return "live:" + liveId + ":throttle:" + userId;
    }
}