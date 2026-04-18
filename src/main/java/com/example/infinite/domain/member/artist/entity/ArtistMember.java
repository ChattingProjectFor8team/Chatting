package com.example.infinite.domain.member.artist.entity;

import com.example.infinite.domain.member.member.entity.Member;
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
@Table(name = "artist_members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE artist_members SET deleted_at = current_timestamp WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class ArtistMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "artist_id", nullable = false)
    private Artist artist;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "stage_name", nullable = false, length = 100)
    private String stageName;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MemberStatus status;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    private ArtistMember(Artist artist, Member member, String stageName, String profileImageUrl, int sortOrder) {
        this.artist = artist;
        this.member = member;
        this.stageName = stageName;
        this.profileImageUrl = profileImageUrl;
        this.status = MemberStatus.ACTIVE;
        this.sortOrder = sortOrder;
    }

    public static ArtistMember create(Artist artist, Member member, String stageName, String profileImageUrl, int sortOrder) {
        // 아티스트 생성자를 첫 멤버로 즉시 연결해 이후 권한 판단의 기준점으로 사용한다.
        return new ArtistMember(artist, member, stageName, profileImageUrl, sortOrder);
    }

    public void updateProfile(String stageName, String profileImageUrl, MemberStatus status, int sortOrder) {
        // 아티스트 멤버 프로필/상태/정렬값을 한 번에 갱신한다.
        this.stageName = stageName;
        this.profileImageUrl = profileImageUrl;
        this.status = status;
        this.sortOrder = sortOrder;
    }
}
