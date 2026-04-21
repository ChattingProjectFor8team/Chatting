package com.example.infinite.domain.artistcontent.post.fanletter.dto.response;

import com.example.infinite.domain.artistcontent.post.fanletter.enums.FanLetterRecipientType;

import java.time.LocalDateTime;

// 팬레터 목록 카드 전용 응답이다.
// 목록에서는 작성자 프로필/배지를 숨기고, 이미지/수신자/special-like 정보만 내려준다.
public record FanLetterListResponse(
        Long fanLetterId,
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
    public static FanLetterListResponse from(
            FanLetterListRow row,
            FanLetterImageResponse image,
            boolean artistLiked,
            String artistLikeDisplayName,
            String artistLikeProfileImageUrl
    ) {
        return new FanLetterListResponse(
                row.fanLetterId(),
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
