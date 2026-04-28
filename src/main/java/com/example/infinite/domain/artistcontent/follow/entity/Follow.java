package com.example.infinite.domain.artistcontent.follow.entity;

import com.example.infinite.domain.member.artist.entity.ArtistMember;
import com.example.infinite.domain.member.member.entity.Member;
import com.example.infinite.global.common.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// Follow는 "내가 지금 이 ArtistMember를 보고 있는가"만 중요해서
// 재팔로우 시 unique 충돌/복구 정책이 복잡해지지 않도록 hard delete 토글을 사용한다.
@Table(
        name = "follows",
        indexes = {
                @Index(name = "idx_follows_follower_id_id", columnList = "follower_member_id, id"),
                @Index(name = "idx_follows_target_artist_member_id", columnList = "target_artist_member_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_follow_follower_target_artist_member",
                        columnNames = {"follower_member_id", "target_artist_member_id"}
                )
        }
)
public class Follow extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "follower_member_id", nullable = false)
    private Member followerMember;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_artist_member_id", nullable = false)
    private ArtistMember targetArtistMember;

    public static Follow create(Member followerMember, ArtistMember targetArtistMember) {
        Follow follow = new Follow();
        // 이번 과제의 follow 범위는 "일반 회원 -> 아티스트 멤버" 단방향으로 고정한다.
        follow.followerMember = followerMember;
        follow.targetArtistMember = targetArtistMember;
        return follow;
    }
}
