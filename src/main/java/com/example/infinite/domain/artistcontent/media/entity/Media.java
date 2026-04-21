package com.example.infinite.domain.artistcontent.media.entity;

import com.example.infinite.domain.artistcontent.media.enums.MediaType;
import com.example.infinite.domain.artistcontent.post.eunms.PostType;
import com.example.infinite.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "media_files",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_media_target_sort_order",
                columnNames = {"target_type", "target_id", "sort_order"}
        ),
        indexes = {
                @Index(name = "idx_media_target", columnList = "target_type, target_id"),
                @Index(name = "idx_media_target_type", columnList = "target_type, media_type")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
/*
 * 게시글에 붙는 첨부파일의 "메타데이터"만 저장하는 엔티티다.
 *
 * 중요한 설계 포인트:
 * - 실제 바이너리 파일은 S3 같은 object storage 에 있다.
 * - DB 에는 조회/정렬/렌더링에 필요한 정보만 남긴다.
 * - FanPost / ArtistPost 를 모두 수용하기 위해 직접 연관관계 대신
 *   targetType + targetId 조합으로 어느 게시글의 미디어인지 식별한다.
 */
public class Media extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private PostType targetType;

    // targetType 이 가리키는 실제 게시글 id 다.
    // 예: FAN_POST + 15, ARTIST_POST + 8
    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 30)
    private MediaType mediaType;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    private Media(
            PostType targetType,
            Long targetId,
            MediaType mediaType,
            String storageKey,
            String fileUrl,
            String thumbnailUrl,
            String originalFileName,
            String contentType,
            long fileSize,
            int sortOrder
    ) {
        // 생성자는 create() 로 감추고,
        // target/media/url 메타데이터가 모두 준비된 경우만 row 를 만들게 강제한다.
        this.targetType = targetType;
        this.targetId = targetId;
        this.mediaType = mediaType;
        this.storageKey = storageKey;
        this.fileUrl = fileUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.sortOrder = sortOrder;
    }

    public static Media create(
            PostType targetType,
            Long targetId,
            MediaType mediaType,
            String storageKey,
            String fileUrl,
            String thumbnailUrl,
            String originalFileName,
            String contentType,
            long fileSize,
            int sortOrder
    ) {
        // 게시글 조회 시 프론트가 그대로 사용할 수 있도록
        // 업로드 결과와 노출 순서를 메타데이터로 저장한다.
        // 특히 targetType + targetId + sortOrder 조합이 실제 렌더링 순서를 결정한다.
        return new Media(
                targetType,
                targetId,
                mediaType,
                storageKey,
                fileUrl,
                thumbnailUrl,
                originalFileName,
                contentType,
                fileSize,
                sortOrder
        );
    }
}
