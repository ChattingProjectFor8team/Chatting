package com.example.infinite.domain.dm.dto.response;

import com.example.infinite.domain.dm.entity.DmMessage;
import com.example.infinite.domain.dm.enums.SenderType;

import java.time.LocalDateTime;

public record DmMessageResponse(
        Long id,
        Long roomId,
        SenderType senderType,
        String content,
        LocalDateTime sentAt
) {
    public static DmMessageResponse from(DmMessage msg) {
        return new DmMessageResponse(
                msg.getId(),
                msg.getDmRoom().getId(),
                msg.getSenderType(),
                msg.getContent(),
                msg.getSentAt()
        );
    }
}
