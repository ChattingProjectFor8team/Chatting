package com.example.infinite.domain.ArtistContent.Post.ArtistPost.Entity;

import com.example.infinite.domain.ArtistContent.Post.eunms.PostVisibility;
import com.example.infinite.domain.Member.Entity.Artist;
import com.example.infinite.domain.Member.Entity.Member;
import com.example.infinite.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Entity
@Table(name = "artist_posts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE artist_posts SET deleted_at = current_timestamp WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class ArtistPost extends BaseEntity {

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
