package com.example.infinite.domain.artistcontent.media.entity;

import com.example.infinite.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "artist_youtube_videos",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_artist_youtube_video",
                columnNames = {"artist_id", "youtube_video_id"}
        ),
        indexes = {
                @Index(name = "idx_artist_youtube_videos_artist_id", columnList = "artist_id, id"),
                @Index(name = "idx_artist_youtube_videos_writer_id", columnList = "writer_member_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
/*
 * 게시글 첨부 Media 와 별개로, 아티스트 탭에 노출할 유튜브 영상 메타데이터만 저장한다.
 *
 * 설계 포인트:
 * - 실제 영상 파일은 유튜브에 있고 DB 에는 카드 렌더링에 필요한 값만 남긴다.
 * - 작성자 표시가 나중에 바뀌지 않도록 활동명/프로필 URL 을 스냅샷으로 저장한다.
 * - 중복 영상 재등록 정책을 단순하게 유지하기 위해 YouTube 카드 엔티티는 hard delete 전제로 둔다.
 * - 목록은 id DESC 커서 슬라이스로 읽기 때문에 artist_id + id 인덱스를 둔다.
 */
public class ArtistYoutubeVideo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "artist_id", nullable = false)
    private Long artistId;

    @Column(name = "writer_member_id", nullable = false)
    private Long writerMemberId;

    @Column(name = "writer_display_name", nullable = false, length = 100)
    private String writerDisplayName;

    @Column(name = "writer_profile_image_url", length = 500)
    private String writerProfileImageUrl;

    @Column(name = "youtube_video_id", nullable = false, length = 30)
    private String youtubeVideoId;

    @Column(name = "youtube_url", nullable = false, length = 500)
    private String youtubeUrl;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "thumbnail_url", nullable = false, length = 500)
    private String thumbnailUrl;

    @Column(name = "duration_seconds", nullable = false)
    private long durationSeconds;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    private ArtistYoutubeVideo(
            Long artistId,
            Long writerMemberId,
            String writerDisplayName,
            String writerProfileImageUrl,
            String youtubeVideoId,
            String youtubeUrl,
            String title,
            String thumbnailUrl,
            long durationSeconds,
            LocalDateTime publishedAt
    ) {
        this.artistId = artistId;
        this.writerMemberId = writerMemberId;
        this.writerDisplayName = writerDisplayName;
        this.writerProfileImageUrl = writerProfileImageUrl;
        this.youtubeVideoId = youtubeVideoId;
        this.youtubeUrl = youtubeUrl;
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
        this.durationSeconds = durationSeconds;
        this.publishedAt = publishedAt;
    }

    public static ArtistYoutubeVideo create(
            Long artistId,
            Long writerMemberId,
            String writerDisplayName,
            String writerProfileImageUrl,
            String youtubeVideoId,
            String youtubeUrl,
            String title,
            String thumbnailUrl,
            long durationSeconds,
            LocalDateTime publishedAt
    ) {
        return new ArtistYoutubeVideo(
                artistId,
                writerMemberId,
                writerDisplayName,
                writerProfileImageUrl,
                youtubeVideoId,
                youtubeUrl,
                title,
                thumbnailUrl,
                durationSeconds,
                publishedAt
        );
    }
}
