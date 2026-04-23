package com.example.infinite.domain.artistcontent.post.fanpost.repository;

import com.example.infinite.domain.artistcontent.post.cache.PostHotRow;
import com.example.infinite.domain.artistcontent.post.fanpost.entity.FanPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FanPostRepository extends JpaRepository<FanPost, Long>, FanPostRepositoryCustom {
    Optional<FanPost> findByIdAndArtistId(Long fanPostId, Long artistId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update FanPost fanPost
               set fanPost.likeCount =
                   case
                       when fanPost.likeCount + :delta < 0 then 0
                       else fanPost.likeCount + :delta
                   end
             where fanPost.id = :fanPostId
            """)
    int changeLikeCountBy(@Param("fanPostId") Long fanPostId, @Param("delta") long delta);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update FanPost fanPost
               set fanPost.commentCount =
                   case
                       when fanPost.commentCount + :delta < 0 then 0
                       else fanPost.commentCount + :delta
                   end
             where fanPost.id = :fanPostId
            """)
    int changeCommentCountBy(@Param("fanPostId") Long fanPostId, @Param("delta") long delta);

    @Query("select fanPost.likeCount from FanPost fanPost where fanPost.id = :fanPostId")
    Optional<Long> findLikeCountById(@Param("fanPostId") Long fanPostId);

    @Query("""
            select new com.example.infinite.domain.artistcontent.post.cache.PostHotRow(
                fanPost.id,
                fanPost.likeCount,
                fanPost.commentCount
            )
              from FanPost fanPost
             where fanPost.id in :fanPostIds
            """)
    List<PostHotRow> findHotRowsByIds(@Param("fanPostIds") Collection<Long> fanPostIds);
}
