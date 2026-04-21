package com.example.infinite.domain.realtimelive.service;

import com.example.infinite.domain.realtimelive.dto.request.LiveCreateRequest;
import com.example.infinite.domain.realtimelive.dto.response.LiveChatMessageResponse;
import com.example.infinite.domain.realtimelive.dto.response.LiveResponse;
import com.example.infinite.domain.realtimelive.entity.LiveChatMessage;
import com.example.infinite.domain.realtimelive.entity.RealtimeLive;
import com.example.infinite.domain.realtimelive.enums.LiveStatus;
import com.example.infinite.domain.realtimelive.error.LiveErrorCode;
import com.example.infinite.domain.realtimelive.error.LiveException;
import com.example.infinite.domain.realtimelive.repository.LiveChatMessageRepository;
import com.example.infinite.domain.realtimelive.repository.RealtimeLiveRepository;
import com.example.infinite.global.common.dto.CursorSliceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RealtimeLiveService {

    private static final String LIVE_STATUS_KEY = "live:%d:status";

    private final RealtimeLiveRepository realtimeLiveRepository;
    private final LiveChatMessageRepository liveChatMessageRepository;
    private final LiveChatBroadcastService liveChatBroadcastService;
    private final StringRedisTemplate stringRedisTemplate;

    @Transactional
    public LiveResponse createLive(Long artistId, LiveCreateRequest request) {
        RealtimeLive live = RealtimeLive.builder()
                .artistId(artistId)
                .title(request.title())
                .description(request.description())
                .thumbnailUrl(request.thumbnailUrl())
                .build();
        return LiveResponse.from(realtimeLiveRepository.save(live));
    }

    @Transactional
    public LiveResponse startLive(Long artistId, Long liveId) {
        RealtimeLive live = findLiveOrThrow(liveId);
        validateOwnership(live, artistId);

        if (live.getLiveStatus() != LiveStatus.SCHEDULED) {
            throw new LiveException(LiveErrorCode.LIVE_NOT_SCHEDULED);
        }

        live.start();

        stringRedisTemplate.opsForValue()
                .set(String.format(LIVE_STATUS_KEY, liveId), "LIVE");

        return LiveResponse.from(live);
    }

    @Transactional
    public LiveResponse endLive(Long artistId, Long liveId) {
        RealtimeLive live = findLiveOrThrow(liveId);
        validateOwnership(live, artistId);

        if (live.getLiveStatus() != LiveStatus.LIVE) {
            throw new LiveException(LiveErrorCode.LIVE_NOT_LIVE);
        }

        live.end();

        stringRedisTemplate.delete(String.format(LIVE_STATUS_KEY, liveId));

        liveChatBroadcastService.drainAndRemove(liveId);

        return LiveResponse.from(live);
    }

    @Transactional
    public void deleteChatMessage(Long artistId, Long liveId, Long messageId) {
        RealtimeLive live = findLiveOrThrow(liveId);
        validateOwnership(live, artistId);

        LiveChatMessage message = liveChatMessageRepository.findById(messageId)
                .orElseThrow(() -> new LiveException(LiveErrorCode.CHAT_MESSAGE_NOT_FOUND));

        if (message.getDeletedAt() != null) {
            throw new LiveException(LiveErrorCode.MESSAGE_ALREADY_DELETED);
        }

        message.softDelete();
    }

    public List<LiveResponse> getLiveList(Long artistId, LiveStatus status) {
        List<RealtimeLive> lives = (status != null)
                ? realtimeLiveRepository.findByArtistIdAndLiveStatusOrderByCreatedAtDesc(artistId, status)
                : realtimeLiveRepository.findByArtistIdOrderByCreatedAtDesc(artistId);
        return lives.stream().map(LiveResponse::from).toList();
    }

    public LiveResponse getLiveDetail(Long artistId, Long liveId) {
        RealtimeLive live = findLiveOrThrow(liveId);
        if (!live.getArtistId().equals(artistId)) {
            throw new LiveException(LiveErrorCode.LIVE_NOT_FOUND);
        }
        return LiveResponse.from(live);
    }

    public CursorSliceResponse<LiveChatMessageResponse> getChatMessages(
            Long liveId, Long cursor, int size) {
        findLiveOrThrow(liveId);

        int effectiveSize = Math.min(Math.max(size, 1), 100);

        List<LiveChatMessage> rows = liveChatMessageRepository
                .findByCursor(liveId, cursor, effectiveSize + 1);

        List<LiveChatMessageResponse> mapped = rows.stream()
                .map(LiveChatMessageResponse::from)
                .toList();

        return CursorSliceResponse.of(mapped, effectiveSize, LiveChatMessageResponse::id);
    }

    public boolean isLiveActive(Long liveId) {
        return Boolean.TRUE.equals(
                stringRedisTemplate.hasKey(String.format(LIVE_STATUS_KEY, liveId))
        );
    }

    private RealtimeLive findLiveOrThrow(Long liveId) {
        return realtimeLiveRepository.findById(liveId)
                .orElseThrow(() -> new LiveException(LiveErrorCode.LIVE_NOT_FOUND));
    }

    private void validateOwnership(RealtimeLive live, Long artistId) {
        if (!live.getArtistId().equals(artistId)) {
            throw new LiveException(LiveErrorCode.NOT_LIVE_OWNER);
        }
    }
}
