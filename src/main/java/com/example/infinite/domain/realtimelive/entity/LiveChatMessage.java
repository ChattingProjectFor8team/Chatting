package com.example.infinite.domain.realtimelive.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "live_chat_messages", indexes = {
        @Index(name = "idx_chat_live_created", columnList = "live_stream_id, created_at")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE live_chat_messages SET deleted_at = current_timestamp WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class LiveChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "live_stream_id", nullable = false)
    private Long liveStreamId;

    @Column(name = "sender_user_id", nullable = false)
    private Long senderUserId;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    private LiveChatMessage(Long liveStreamId, Long senderUserId, String message) {
        this.liveStreamId = liveStreamId;
        this.senderUserId = senderUserId;
        this.message = message;
        this.createdAt = LocalDateTime.now();
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}