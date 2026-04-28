package com.example.infinite.domain.artistcontent.post.fanletter.entity;

import com.example.infinite.domain.artistcontent.post.fanletter.enums.FanLetterRecipientType;
import com.example.infinite.domain.member.artist.entity.Artist;
import com.example.infinite.domain.member.artist.entity.ArtistMember;
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
        name = "fan_letter",
        indexes = {
                @Index(name = "idx_fan_letter_artist_id_id", columnList = "artist_id, id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE fan_letter SET deleted_at = current_timestamp WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
// 팬레터는 일반 게시글처럼 본문/댓글 중심 모델이 아니라
// "작성자 + 수신 대상 + 이미지 1장 + 좋아요"에 집중된 가벼운 도메인이다.
public class FanLetter extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "artist_id", nullable = false)
    private Artist artist;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member writer;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type", nullable = false, length = 30)
    private FanLetterRecipientType recipientType;

    // recipientType=ARTIST_MEMBER 일 때만 값이 있고,
    // ARTIST 인 경우에는 null 로 두고 artist 자체를 수신 대상으로 해석한다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_artist_member_id")
    private ArtistMember recipientArtistMember;

    @Column(nullable = false)
    private long likeCount = 0L;

    private FanLetter(Artist artist, Member writer, FanLetterRecipientType recipientType, ArtistMember recipientArtistMember) {
        this.artist = artist;
        this.writer = writer;
        this.recipientType = recipientType;
        this.recipientArtistMember = recipientArtistMember;
    }

    public static FanLetter create(
            Artist artist,
            Member writer,
            FanLetterRecipientType recipientType,
            ArtistMember recipientArtistMember
    ) {
        // 팬레터는 본문 텍스트 없이 "누구에게 보내는가 + 이미지 한 장"을 중심으로 저장한다.
        return new FanLetter(artist, writer, recipientType, recipientArtistMember);
    }

    public void updateRecipient(FanLetterRecipientType recipientType, ArtistMember recipientArtistMember) {
        // 수정 시에도 recipientType 과 recipientArtistMember 를 항상 함께 정규화해서 맞춘다.
        this.recipientType = recipientType;
        this.recipientArtistMember = recipientArtistMember;
    }

    public void changeLikeCountBy(int delta) {
        this.likeCount = Math.max(0, this.likeCount + delta);
    }

}
