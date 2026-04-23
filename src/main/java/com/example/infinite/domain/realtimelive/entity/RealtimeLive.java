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
/*
 * 실시간 방송 메타데이터와 종료 후 다시보기(VOD) 공개 상태를 함께 담는 엔티티다.
 *
 * 이번 최소 수정안의 핵심:
 * - 채팅/소켓 구조는 그대로 둔다.
 * - 방송 생성 시 host 스냅샷을 같이 저장해 VOD 카드에 바로 쓸 수 있게 한다.
 * - replayUrl 이 등록되면 REPLAY_READY 상태로 바뀌며, 이 상태만 공개 VOD 목록에 노출한다.
 */
public class RealtimeLive extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "artist_id", nullable = false)
    private Long artistId;

    @Column(name = "host_member_id", nullable = false)
    private Long hostMemberId;

    @Column(name = "host_display_name", nullable = false, length = 100)
    private String hostDisplayName;

    @Column(name = "host_profile_image_url", length = 500)
    private String hostProfileImageUrl;

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

    @Column(name = "replay_published_at")
    private LocalDateTime replayPublishedAt;

    @Builder
    private RealtimeLive(
            Long artistId,
            Long hostMemberId,
            String hostDisplayName,
            String hostProfileImageUrl,
            String title,
            String description,
            String thumbnailUrl
    ) {
        this.artistId = artistId;
        this.hostMemberId = hostMemberId;
        this.hostDisplayName = hostDisplayName;
        this.hostProfileImageUrl = hostProfileImageUrl;
        this.title = title;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.liveStatus = LiveStatus.SCHEDULED;
    }

    public void start() {
        // 실제 방송 시작 시점은 스케줄 생성 시각과 다를 수 있으므로 startedAt을 별도로 기록한다.
        this.liveStatus = LiveStatus.LIVE;
        this.startedAt = LocalDateTime.now();
    }

    public void end() {
        // 종료 시각을 남겨 두면 VOD 목록에서 재생 시간을 계산할 수 있다.
        this.liveStatus = LiveStatus.ENDED;
        this.endedAt = LocalDateTime.now();
    }

    public void markReplayReady(String replayUrl) {
        // 실제 녹화 파일/스트리밍 저장 파이프라인은 외부에 있을 수 있으므로,
        // 현재 시스템은 "replay URL 이 준비된 시점"에 공개 가능 상태로 전환한다.
        this.liveStatus = LiveStatus.REPLAY_READY;
        this.replayUrl = replayUrl;
        this.replayPublishedAt = LocalDateTime.now();
    }
}
