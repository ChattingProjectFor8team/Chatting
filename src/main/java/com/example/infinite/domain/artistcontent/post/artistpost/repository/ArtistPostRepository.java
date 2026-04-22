package com.example.infinite.domain.artistcontent.post.artistpost.repository;

import com.example.infinite.domain.artistcontent.post.artistpost.entity.ArtistPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ArtistPostRepository extends JpaRepository<ArtistPost, Long>, ArtistPostRepositoryCustom {

    Optional<ArtistPost> findByIdAndArtistId(Long artistPostId, Long artistId);

    /**
     * flush 단계에서 사용되는 원자 update.
     *
     * 장점:
     * - 현재 값을 읽어온 뒤 자바에서 +1/-1 하고 저장하는 read-modify-write 경쟁을 피한다
     * - DB가 직접 likeCount = likeCount + delta 를 수행하므로 lost update 위험을 줄인다
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ArtistPost artistPost
               set artistPost.likeCount =
                   case
                       when artistPost.likeCount + :delta < 0 then 0
                       else artistPost.likeCount + :delta
                   end
             where artistPost.id = :artistPostId
            """)
    int changeLikeCountBy(@Param("artistPostId") Long artistPostId, @Param("delta") long delta);

    /**
     * full reconcile 단계에서 사용되는 전수 보정 쿼리.
     *
     * 방식:
     * - reactions 에서 ARTIST_POST + LIKE 개수를 다시 센다
     * - active artist_posts 와 left join 해서 0개 좋아요 글도 함께 맞춘다
     *
     * why native:
     * - 전체 집계 + 조인 update 는 JPQL보다 native가 훨씬 간단하고 명확하다
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update artist_posts artist_post
            left join (
                select reaction.target_id as artist_post_id, count(*) as like_count
                  from reactions reaction
                 where reaction.target_type = 'ARTIST_POST'
                   and reaction.reaction_type = 'LIKE'
                 group by reaction.target_id
            ) reaction_counts
              on reaction_counts.artist_post_id = artist_post.id
               set artist_post.like_count = coalesce(reaction_counts.like_count, 0)
             where artist_post.deleted_at is null
            """, nativeQuery = true)
    int reconcileAllLikeCounts();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ArtistPost artistPost
               set artistPost.commentCount =
                   case
                       when artistPost.commentCount + :delta < 0 then 0
                       else artistPost.commentCount + :delta
                   end
             where artistPost.id = :artistPostId
            """)
    int changeCommentCountBy(@Param("artistPostId") Long artistPostId, @Param("delta") long delta);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update artist_posts artist_post
            left join (
                select comment.target_id as artist_post_id, count(*) as comment_count
                  from comments comment
                 where comment.target_type = 'ARTIST_POST'
                   and comment.deleted_at is null
                   and comment.deleted_placeholder = false
                 group by comment.target_id
            ) comment_counts
              on comment_counts.artist_post_id = artist_post.id
               set artist_post.comment_count = coalesce(comment_counts.comment_count, 0)
             where artist_post.deleted_at is null
            """, nativeQuery = true)
    int reconcileAllCommentCounts();
}
