package com.example.infinite.domain.realtimelive.dto;

import java.time.LocalDateTime;

public record LiveChatMessageDto(
        Long liveId,
        Long senderId,
        String message,
        LocalDateTime sentAt
) {
    public static LiveChatMessageDto of(Long liveId, Long senderId, String message) {
        return new LiveChatMessageDto(liveId, senderId, message, LocalDateTime.now());
    }
}