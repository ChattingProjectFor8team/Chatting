package com.example.infinite.domain.dm.repository;

import com.example.infinite.domain.dm.entity.DmMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DmMessageRepository extends JpaRepository<DmMessage, Long> {

    @Query("""
            SELECT m FROM DmMessage m
            WHERE m.dmRoom.id = :roomId
              AND (:cursor IS NULL OR m.id < :cursor)
            ORDER BY m.id DESC
            LIMIT :limit
            """)
    List<DmMessage> findByRoomIdWithCursor(
            @Param("roomId") Long roomId,
            @Param("cursor") Long cursor,
            @Param("limit") int limit
    );

    @Query("""
            SELECT COUNT(m) FROM DmMessage m
            WHERE m.dmRoom.id = :roomId
              AND m.senderType = com.example.infinite.domain.dm.enums.SenderType.USER
              AND m.id > COALESCE(
                (SELECT MAX(am.id) FROM DmMessage am
                 WHERE am.dmRoom.id = :roomId
                   AND am.senderType = com.example.infinite.domain.dm.enums.SenderType.ARTIST),
                0
              )
            """)
    long countUserMessagesSinceLastArtistReply(@Param("roomId") Long roomId);
}
