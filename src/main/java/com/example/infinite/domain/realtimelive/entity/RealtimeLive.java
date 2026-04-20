package com.example.infinite.domain.realtimelive.entity;

import com.example.infinite.domain.realtimelive.enums.LiveStatus;
import com.example.infinite.global.common.entity.BaseEntity;
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
@Table(name = "realtime_lives", indexes = {
        @Index(name = "idx_live_artist_status", columnList = "artist_id, live_status"),
        @Index(name = "idx_live_artist_created", columnList = "artist_id, created_at")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE realtime_lives SET deleted_at = current_timestamp WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class RealtimeLive extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "artist_id", nullable = false)
    private Long artistId;

    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "live_status", nullable = false, length = 20)
    private LiveStatus liveStatus;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "replay_url", length = 500)
    private String replayUrl;

    @Builder
    private RealtimeLive(Long artistId, String title, String description, String thumbnailUrl) {
        this.artistId = artistId;
        this.title = title;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.liveStatus = LiveStatus.SCHEDULED;
    }

    public void start() {
        this.liveStatus = LiveStatus.LIVE;
        this.startedAt = LocalDateTime.now();
    }

    public void end() {
        this.liveStatus = LiveStatus.ENDED;
        this.endedAt = LocalDateTime.now();
    }

    public void markReplayReady(String replayUrl) {
        this.liveStatus = LiveStatus.REPLAY_READY;
        this.replayUrl = replayUrl;
    }
}