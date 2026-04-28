package com.example.infinite.domain.realtimelive.service;

import com.example.infinite.domain.realtimelive.dto.LiveChatMessageDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * liveId별 인메모리 메시지 버퍼.
 * 메시지 인입 시 큐에 적재, flush 시 큐를 비우고 리스트로 반환.
 *
 * <p>적응형 flush 확장 가이드:
 * 현재는 고정 300ms 간격으로 flush한다.
 * 트래픽 적응형으로 전환하려면:
 * - 큐 사이즈 기반: flush 시점에 큐 사이즈가 N 이상이면 간격을 200ms로 줄이고, 비어있으면 500ms로 늘린다
 * - 메시지 수신률 기반: 최근 1초간 인입 카운터를 두고 임계값으로 간격을 조절한다
 * - 구현 시 @Scheduled를 제거하고 ScheduledExecutorService로 동적 scheduleAtFixedRate를 사용한다
 * </p>
 */
@Component
public class LiveChatBuffer {

    private final Map<Long, ConcurrentLinkedQueue<LiveChatMessageDto>> buffers = new ConcurrentHashMap<>();

    /**
     * 메시지를 해당 liveId 버퍼에 적재한다.
     */
    public void add(LiveChatMessageDto message) {
        buffers.computeIfAbsent(message.liveId(), k -> new ConcurrentLinkedQueue<>())
                .add(message);
    }

    /**
     * 모든 liveId의 버퍼를 flush하여 liveId별 메시지 리스트를 반환한다.
     * 호출 후 버퍼는 비워진다.
     */
    public Map<Long, List<LiveChatMessageDto>> flushAll() {
        Map<Long, List<LiveChatMessageDto>> flushed = new ConcurrentHashMap<>();

        buffers.forEach((liveId, queue) -> {
            List<LiveChatMessageDto> messages = new ArrayList<>();
            LiveChatMessageDto msg;
            while ((msg = queue.poll()) != null) {
                messages.add(msg);
            }
            if (!messages.isEmpty()) {
                flushed.put(liveId, messages);
            }
        });

        return flushed;
    }

    /**
     * 라이브 종료 시 해당 버퍼를 제거한다.
     */
    public void remove(Long liveId) {
        buffers.remove(liveId);
    }

    /**
     * 특정 liveId의 큐를 직접 반환한다. 없으면 null.
     */
    public ConcurrentLinkedQueue<LiveChatMessageDto> getQueue(Long liveId) {
        return buffers.get(liveId);
    }
}