package com.example.infinite.domain.member.artist.entity;

import com.example.infinite.domain.member.member.enums.MemberStatus;
import com.example.infinite.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Entity
@Table(name = "artists")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE artists SET deleted_at = current_timestamp WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Artist extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String name;

    @Column(unique = true, length = 100)
    private String slug;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Lob
    private String intro; // 자기소개

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MemberStatus status;

    private Artist(String name, String slug, String profileImageUrl, String coverImageUrl, String intro) {
        this.name = name;
        this.slug = slug;
        this.profileImageUrl = profileImageUrl;
        this.coverImageUrl = coverImageUrl;
        this.intro = intro;
        this.status = MemberStatus.ACTIVE;
    }

    public static Artist create(String name, String slug, String profileImageUrl, String coverImageUrl, String intro) {
        // 생성 시점에는 기본 공개 상태가 아니라 엔티티 활성 상태만 초기화한다.
        return new Artist(name, slug, profileImageUrl, coverImageUrl, intro);
    }

    public void updateProfile(String name, String slug, String profileImageUrl, String coverImageUrl, String intro) {
        // 아티스트 기본 프로필 수정은 artist 엔티티 내부에서 일관되게 갱신한다.
        this.name = name;
        this.slug = slug;
        this.profileImageUrl = profileImageUrl;
        this.coverImageUrl = coverImageUrl;
        this.intro = intro;
    }
}
