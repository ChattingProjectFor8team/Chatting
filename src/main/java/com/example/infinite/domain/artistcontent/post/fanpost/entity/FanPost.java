package com.example.infinite.domain.artistcontent.post.fanpost.entity;

import com.example.infinite.domain.member.artist.entity.Artist;
import com.example.infinite.domain.member.member.entity.Member;
import com.example.infinite.domain.artistcontent.post.eunms.PostVisibility;
import com.example.infinite.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Entity
@Table(
        name = "fan_posts",
        indexes = {
                @Index(name = "idx_fan_posts_artist_id_id", columnList = "artist_id, id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE fan_posts SET deleted_at = current_timestamp WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class FanPost extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "artist_id", nullable = false)
    private Artist artist;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member writer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PostVisibility visibility;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private long likeCount = 0L;

    @Column(nullable = false)
    private long commentCount = 0L;

    @Column(nullable = false)
    private int mediaCount;

    private FanPost(Artist artist, Member writer, String content) {
        this.artist = artist;
        this.writer = writer;
        // 현재 정책상 팬 게시글은 항상 공개이므로 엔티티 내부에서 고정값으로 관리한다.
        this.visibility = PostVisibility.PUBLIC;
        this.content = content;
        this.mediaCount = 0;
    }

    public static FanPost create(Artist artist, Member writer, String content) {
        // 팬 게시글은 현재 정책상 항상 공개이므로 공개 여부 입력을 받지 않는다.
        return new FanPost(artist, writer, content);
    }

    public void update(String content) {
        // 팬 게시글은 항상 공개이므로 수정 시 본문만 바꾼다.
        this.visibility = PostVisibility.PUBLIC;
        this.content = content;
    }

    /**
     * [Day 12 Step 3] 좋아요 추가 시 +1, 취소 시 -1.
     * 취소 후 음수 방지(비정규화/실패 시나리오에서 최소 0 유지).
     */
    public void changeLikeCountBy(int delta) {
        this.likeCount = Math.max(0, this.likeCount + delta);
    }

    public void changeCommentCountBy(int delta) {
        this.commentCount = Math.max(0, this.commentCount + delta);
    }

    public void changeMediaCountBy(int delta) {
        this.mediaCount = Math.max(0, this.mediaCount + delta);
    }

}
