package com.example.infinite.domain.artistcontent.post.fanletter.repository;

import com.example.infinite.domain.artistcontent.post.cache.PostHotRow;
import com.example.infinite.domain.artistcontent.post.fanletter.entity.FanLetter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FanLetterRepository extends JpaRepository<FanLetter, Long>, FanLetterRepositoryCustom {

    // 팬레터 상세/수정/삭제는 항상 artist 범위 안에서 조회해야
    // 다른 아티스트의 팬레터를 잘못 건드리지 않는다.
    Optional<FanLetter> findByIdAndArtistId(Long fanLetterId, Long artistId);

    Optional<FanLetter> findByArtistIdAndWriterIdAndRecipientTypeAndRecipientArtistMemberIsNull(
            Long artistId,
            Long writerId,
            com.example.infinite.domain.artistcontent.post.fanletter.enums.FanLetterRecipientType recipientType
    );

    Optional<FanLetter> findByArtistIdAndWriterIdAndRecipientTypeAndRecipientArtistMemberId(
            Long artistId,
            Long writerId,
            com.example.infinite.domain.artistcontent.post.fanletter.enums.FanLetterRecipientType recipientType,
            Long recipientArtistMemberId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update FanLetter fanLetter
               set fanLetter.likeCount =
                   case
                       when fanLetter.likeCount + :delta < 0 then 0
                       else fanLetter.likeCount + :delta
                   end
             where fanLetter.id = :fanLetterId
            """)
    int changeLikeCountBy(@Param("fanLetterId") Long fanLetterId, @Param("delta") long delta);

    @Query("select fanLetter.likeCount from FanLetter fanLetter where fanLetter.id = :fanLetterId")
    Optional<Long> findLikeCountById(@Param("fanLetterId") Long fanLetterId);

    @Query("""
            select new com.example.infinite.domain.artistcontent.post.cache.PostHotRow(
                fanLetter.id,
                fanLetter.likeCount
            )
              from FanLetter fanLetter
             where fanLetter.id in :fanLetterIds
            """)
    List<PostHotRow> findHotRowsByIds(@Param("fanLetterIds") Collection<Long> fanLetterIds);
}
