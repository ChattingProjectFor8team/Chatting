package com.example.infinite.domain.artistcontent.interaction.entity;

import com.example.infinite.domain.artistcontent.post.eunms.PostType;
import com.example.infinite.domain.artistcontent.interaction.enums.ReactionType;
import com.example.infinite.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "reactions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_reaction_member_target_type",
                columnNames = {"member_id", "target_type", "target_id", "reaction_type"}
        ),
        indexes = {
                @Index(name = "idx_reaction_target", columnList = "target_type, target_id"),
                @Index(name = "idx_reaction_target_type", columnList = "target_type, target_id, reaction_type"),
                // 반응 주체는 Member 단일 축이므로 member_id 단일 인덱스를 둔다.
                @Index(name = "idx_reaction_member", columnList = "member_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private PostType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    // 반응 주체는 Member 단일 principal 기준으로 저장한다.
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reaction_type", nullable = false, length = 30)
    private ReactionType reactionType;

    private Reaction(PostType targetType, Long targetId, Long memberId, ReactionType reactionType) {
        this.targetType = targetType;
        this.targetId = targetId;
        this.memberId = memberId;
        this.reactionType = reactionType;
    }

    public static Reaction create(PostType targetType, Long targetId, Long memberId, ReactionType reactionType) {
        // 대상 타입과 대상 id를 함께 고정해 중복 반응을 유니크 제약과 함께 막는다.
        return new Reaction(targetType, targetId, memberId, reactionType);
    }
}
