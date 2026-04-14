package com.example.infinite.domain.ArtistContent.Hashtag.Entity;

import com.example.infinite.domain.ArtistContent.Post.eunms.PostType;
import com.example.infinite.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "content_hashtags",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_content_hashtag_target",
                columnNames = {"target_type", "target_id", "hashtag_id"}
        ),
        indexes = {
                @Index(name = "idx_content_hashtag_target", columnList = "target_type, target_id"),
                @Index(name = "idx_content_hashtag_hashtag_id", columnList = "hashtag_id"),
                @Index(name = "idx_content_hashtag_target_hashtag", columnList = "target_type, hashtag_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContentHashtag extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private PostType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hashtag_id", nullable = false)
    private Hashtag hashtag;
}
