package com.example.infinite.domain.realtimelive.repository;

import com.example.infinite.domain.realtimelive.entity.LiveChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LiveChatMessageRepository extends JpaRepository<LiveChatMessage, Long> {

    @Query("""
            SELECT m FROM LiveChatMessage m
            WHERE m.liveStreamId = :liveStreamId
              AND (:cursor IS NULL OR m.id < :cursor)
            ORDER BY m.id DESC
            LIMIT :limit
            """)
    List<LiveChatMessage> findByCursor(
            @Param("liveStreamId") Long liveStreamId,
            @Param("cursor") Long cursor,
            @Param("limit") int limit
    );
}
