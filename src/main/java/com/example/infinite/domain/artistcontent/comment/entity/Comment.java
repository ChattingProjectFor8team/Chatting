package com.example.infinite.domain.artistcontent.comment.entity;

import com.example.infinite.domain.artistcontent.post.eunms.PostType;
import com.example.infinite.domain.member.entity.Member;
import com.example.infinite.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Entity
@Table(name = "comments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE comments SET deleted_at = current_timestamp WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Comment extends BaseEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private PostType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    /**
     * 부모 댓글. null이면 원댓글(Root).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @Column(nullable = false)
    private int depth;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member writer;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;


}
