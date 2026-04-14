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
public class Media extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private PostType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 30)
    private MediaType mediaType;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
