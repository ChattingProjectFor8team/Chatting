package com.example.infinite.domain.dm.dto.response;

import com.example.infinite.domain.dm.entity.DmRoom;

import java.time.LocalDateTime;

public record DmRoomResponse(
        Long id,
        Long userId,
        Long artistId,
        LocalDateTime updatedAt
) {
    public static DmRoomResponse from(DmRoom room) {
        return new DmRoomResponse(
                room.getId(),
                room.getUserId(),
                room.getArtistId(),
                room.getUpdatedAt()
        );
    }
}
