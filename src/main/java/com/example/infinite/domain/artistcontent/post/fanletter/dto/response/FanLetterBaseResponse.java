package com.example.infinite.domain.artistcontent.post.fanletter.dto.response;

import com.example.infinite.domain.artistcontent.post.fanletter.enums.FanLetterRecipientType;
import com.example.infinite.domain.subscriptionmembership.dto.response.WriterSubscriptionBadge;

import java.time.LocalDateTime;

/**
 * fan letter 상세 응답에서 변동이 적은 base 필드만 모아둔 DTO다.
 *
 * fan letter는 댓글이 없으므로 hot 필드는 likeCount만 분리한다.
 */
public record FanLetterBaseResponse(
        Long fanLetterId,
        Long artistId,
        Long writerId,
        String writerNickname,
        String writerProfileImageUrl,
        boolean fanMembershipSubscribed,
        boolean dmSubscribed,
        FanLetterRecipientType recipientType,
        Long recipientArtistMemberId,
        String recipientDisplayName,
        String recipientProfileImageUrl,
        FanLetterImageResponse image,
        boolean artistLiked,
        String artistLikeDisplayName,
        String artistLikeProfileImageUrl,
        LocalDateTime createdAt
) {
    public static FanLetterBaseResponse from(
            FanLetterReadRow row,
            FanLetterImageResponse image,
            WriterSubscriptionBadge writerBadge,
            boolean artistLiked,
            String artistLikeDisplayName,
            String artistLikeProfileImageUrl
    ) {
        return new FanLetterBaseResponse(
                row.fanLetterId(),
                row.artistId(),
                row.writerId(),
                row.writerNickname(),
                row.writerProfileImageUrl(),
                writerBadge.fanMembershipSubscribed(),
                writerBadge.dmSubscribed(),
                row.recipientType(),
                row.recipientArtistMemberId(),
                row.recipientDisplayName(),
                row.recipientProfileImageUrl(),
                image,
                artistLiked,
                artistLikeDisplayName,
                artistLikeProfileImageUrl,
                row.createdAt()
        );
    }
}
