package com.example.infinite.domain.artistcontent.interaction.repository;

import com.example.infinite.domain.artistcontent.interaction.entity.Reaction;
import com.example.infinite.domain.artistcontent.interaction.enums.ReactionType;
import com.example.infinite.domain.artistcontent.post.enums.PostType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InteractionRepository extends JpaRepository<Reaction, Long>, InteractionRepositoryCustom {

    // 반응 중복 여부는 Member(memberId) + target + reactionType 조합으로 판단한다.
    boolean existsByTargetTypeAndTargetIdAndMemberIdAndReactionType(
            PostType targetType,
            Long targetId,
            Long memberId,
            ReactionType reactionType
    );

    // 팬레터 특수 좋아요 여부도 같은 Reaction 레코드를 조회한 뒤 member의 role로 해석한다.
    Optional<Reaction> findByTargetTypeAndTargetIdAndMemberIdAndReactionType(
            PostType targetType,
            Long targetId,
            Long memberId,
            ReactionType reactionType
    );

    long countByTargetTypeAndTargetIdAndReactionType(
            PostType targetType,
            Long targetId,
            ReactionType reactionType
    );

    // 팬레터 special-like 계산처럼 "여러 대상의 좋아요를 한 번에 읽어야 하는" 배치 조회에 사용한다.
    List<Reaction> findByTargetTypeAndTargetIdInAndReactionType(
            PostType targetType,
            Collection<Long> targetIds,
            ReactionType reactionType
    );

    /**
     * 고트래픽 ArtistPost 좋아요 consumer 전용 insert 경로다.
     *
     * 왜 save() 대신 INSERT IGNORE를 쓰는가:
     * - 기존 방식은 "존재 조회 -> 엔티티 save" 2단계라 burst에서 느렸다
     * - 동시에 같은 desired=true 명령이 재처리되면 unique key 충돌이 터질 수 있었다
     * - INSERT IGNORE는 DB가 unique 충돌을 no-op으로 처리해 주므로
     *   멱등성과 처리량을 같이 챙길 수 있다
     *
     * 반환값 해석:
     * - 1: 실제로 새 Reaction row가 들어감
     * - 0: 이미 row가 있었으므로 no-op
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            insert ignore into reactions (
                created_at,
                updated_at,
                deleted_at,
                member_id,
                reaction_type,
                target_id,
                target_type
            ) values (
                current_timestamp,
                current_timestamp,
                null,
                :memberId,
                :reactionType,
                :targetId,
                :targetType
            )
            """, nativeQuery = true)
    int insertIgnore(
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId,
            @Param("memberId") Long memberId,
            @Param("reactionType") String reactionType
    );

    /**
     * 고트래픽 ArtistPost 좋아요 consumer 전용 delete 경로다.
     *
     * 왜 엔티티 조회 후 delete() 대신 조건부 delete 쿼리를 쓰는가:
     * - desired=false 명령도 멱등해야 한다
     * - row가 없으면 그냥 0건 삭제로 끝나면 되고 예외가 필요 없다
     * - "조회 후 엔티티 삭제"보다 round-trip이 적어 consumer 처리량이 더 좋다
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from Reaction reaction
             where reaction.targetType = :targetType
               and reaction.targetId = :targetId
               and reaction.memberId = :memberId
               and reaction.reactionType = :reactionType
            """)
    int deleteIfExists(
            @Param("targetType") PostType targetType,
            @Param("targetId") Long targetId,
            @Param("memberId") Long memberId,
            @Param("reactionType") ReactionType reactionType
    );
}
