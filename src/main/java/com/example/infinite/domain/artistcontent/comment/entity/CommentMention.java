package com.example.infinite.domain.artistcontent.comment.entity;

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

@Getter
@Entity
@Table(
        name = "comment_mentions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_comment_mention",
                columnNames = {"comment_id", "mentioned_member_id"}
        ),
        indexes = {
                @Index(name = "idx_comment_mentions_comment_id", columnList = "comment_id"),
                @Index(name = "idx_comment_mentions_member_id", columnList = "mentioned_member_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentMention extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 댓글 본문에 등장한 멘션과 실제 멘션 대상 Member를 분리 저장해 알림/재동기화에 재사용한다.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mentioned_member_id", nullable = false)
    private Member mentionedMember;

    private CommentMention(Comment comment, Member mentionedMember) {
        this.comment = comment;
        this.mentionedMember = mentionedMember;
    }

    public static CommentMention create(Comment comment, Member mentionedMember) {
        // 댓글 본문에서 실제로 해석된 멘션 대상만 별도 매핑 테이블에 남긴다.
        return new CommentMention(comment, mentionedMember);
    }
}
