package com.example.infinite.domain.artistcontent.comment.entity;

import com.example.infinite.domain.artistcontent.post.enums.PostType;
import com.example.infinite.domain.member.member.entity.Member;
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
        name = "comments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_comments_command_request_id", columnNames = "command_request_id")
        },
        indexes = {
                @Index(name = "idx_comments_target", columnList = "target_type, target_id, id"),
                @Index(name = "idx_comments_parent", columnList = "parent_id, id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE comments SET deleted_at = current_timestamp WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Comment extends BaseEntity {

    private static final String DELETED_PLACEHOLDER_CONTENT = "삭제된 댓글입니다.";

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

    @Column(nullable = false)
    private long likeCount;

    @Column(name = "command_request_id", length = 64)
    private String commandRequestId;

    @Column(name = "deleted_placeholder", nullable = false)
    private boolean deletedPlaceholder;

    private Comment(PostType targetType, Long targetId, Comment parent, int depth, Member writer, String content, String commandRequestId) {
        this.targetType = targetType;
        this.targetId = targetId;
        this.parent = parent;
        this.depth = depth;
        this.writer = writer;
        this.content = content;
        this.likeCount = 0L;
        this.commandRequestId = commandRequestId;
        this.deletedPlaceholder = false;
    }

    public static Comment create(PostType targetType, Long targetId, Member writer, String content, Comment parent) {
        return create(targetType, targetId, writer, content, parent, null);
    }

    public static Comment create(
            PostType targetType,
            Long targetId,
            Member writer,
            String content,
            Comment parent,
            String commandRequestId
    ) {
        // 원댓글은 depth 1, 대댓글은 depth 2로만 생성해 3-depth 이상이 들어오지 않게 서비스와 규칙을 맞춘다.
        return new Comment(targetType, targetId, parent, parent == null ? 1 : 2, writer, content, commandRequestId);
    }

    public boolean isRootComment() {
        return parent == null;
    }

    public boolean isOwnedBy(Long memberId) {
        return writer.getId().equals(memberId);
    }

    public void markDeletedPlaceholder() {
        this.content = DELETED_PLACEHOLDER_CONTENT;
        this.deletedPlaceholder = true;
    }

    public void changeLikeCountBy(int delta) {
        this.likeCount = Math.max(0L, this.likeCount + delta);
    }

}
