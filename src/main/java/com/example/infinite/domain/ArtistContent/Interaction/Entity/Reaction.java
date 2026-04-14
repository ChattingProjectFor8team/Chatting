package com.example.infinite.domain.ArtistContent.Interaction.Entity;

import com.example.infinite.domain.ArtistContent.Post.eunms.PostType;
import com.example.infinite.domain.ArtistContent.Interaction.enums.ReactionType;
import com.example.infinite.domain.Member.enums.MemberType;
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
                name = "uk_reaction_actor_target_type",
                columnNames = {"actor_type", "actor_id", "target_type", "target_id", "reaction_type"}
        ),
        indexes = {
                @Index(name = "idx_reaction_target", columnList = "target_type, target_id"),
                @Index(name = "idx_reaction_target_type", columnList = "target_type, target_id, reaction_type"),
                @Index(name = "idx_reaction_actor", columnList = "actor_type, actor_id")
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

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 30)
    private MemberType actorType;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reaction_type", nullable = false, length = 30)
    private ReactionType reactionType;
}
