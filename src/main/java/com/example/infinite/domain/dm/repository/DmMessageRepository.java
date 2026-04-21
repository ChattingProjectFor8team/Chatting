package com.example.infinite.domain.dm.repository;

import com.example.infinite.domain.dm.entity.DmMessage;
import com.example.infinite.domain.dm.enums.SenderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

    /**
     * 특정 유저가 특정 아티스트와의 DM 룸에서,
     * 지정 시각 이후에 USER 타입 메시지를 보낸 적이 있는지 확인.
     * 구독권 환불 검증용 — 결제 도메인에서 DmService를 통해 호출.
     */
    boolean existsByDmRoom_UserIdAndDmRoom_ArtistIdAndSenderTypeAndSentAtAfter(
            Long userId, Long artistId, SenderType senderType, LocalDateTime after
    );
}
