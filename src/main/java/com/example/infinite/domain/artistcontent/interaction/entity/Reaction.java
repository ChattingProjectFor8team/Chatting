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
                columnNames = {"actor_id", "target_type", "target_id", "reaction_type"}
        ),
        indexes = {
                @Index(name = "idx_reaction_target", columnList = "target_type, target_id"),
                @Index(name = "idx_reaction_target_type", columnList = "target_type, target_id, reaction_type"),
                // 반응 주체는 이제 Member 단일 축으로 보므로 actor_id만 인덱스로 둔다.
                @Index(name = "idx_reaction_actor", columnList = "actor_id")
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
    // 팬레터의 아티스트 특수 표시는 별도 type 컬럼이 아니라 Member role / ArtistMember 연결로 판단한다.
    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reaction_type", nullable = false, length = 30)
    private ReactionType reactionType;
}
