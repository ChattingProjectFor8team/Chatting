package com.example.infinite.domain.artistcontent.hashtag.entity;

import com.example.infinite.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "hashtags",
        uniqueConstraints = @UniqueConstraint(name = "uk_hashtag_name", columnNames = "name"),
        indexes = {
                @Index(name = "idx_hashtag_name", columnList = "name"),
                @Index(name = "idx_hashtag_usage_count", columnList = "usage_count")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Hashtag extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "usage_count", nullable = false)
    private long usageCount = 0L;

    @Builder
    private Hashtag(String name) {
        // 해시태그 저장 키는 항상 정규화된 이름을 사용한다.
        this.name = name;
    }

    public static Hashtag create(String normalizedName) {
        return Hashtag.builder()
                .name(normalizedName)
                .build();
    }

    public void incrementUsageCount() {
        this.usageCount++;
    }

    public void decrementUsageCount() {
        this.usageCount = Math.max(0L, this.usageCount - 1);
    }
}
