package com.example.infinite.domain.realtimelive.dto.response;

import com.example.infinite.domain.realtimelive.entity.LiveChatMessage;

import java.time.LocalDateTime;

public record LiveChatMessageResponse(
        Long id,
        Long senderUserId,
        String message,
        LocalDateTime createdAt
) {
    public static LiveChatMessageResponse from(LiveChatMessage entity) {
        return new LiveChatMessageResponse(
                entity.getId(),
                entity.getSenderUserId(),
                entity.getMessage(),
                entity.getCreatedAt()
        );
    }
}
