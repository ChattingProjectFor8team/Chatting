package com.example.infinite.domain.artistcontent.post.fanletter.dto.response;

import com.example.infinite.domain.artistcontent.post.cache.PostHotData;
import com.example.infinite.domain.artistcontent.post.fanletter.enums.FanLetterRecipientType;

import java.time.LocalDateTime;

// FanLetter는 일반 목록과 상세 shape가 의도적으로 다르다.
// HOT 목록은 목록 카드 축을 유지하면서 인기 지표인 likeCount만 추가로 실어 준다.
public record FanLetterHotResponse(
        Long fanLetterId,
        FanLetterRecipientType recipientType,
        Long recipientArtistMemberId,
        String recipientDisplayName,
        String recipientProfileImageUrl,
        FanLetterImageResponse image,
        long likeCount,
        boolean artistLiked,
        String artistLikeDisplayName,
        String artistLikeProfileImageUrl,
        LocalDateTime createdAt
) {
    public static FanLetterHotResponse from(FanLetterListResponse baseResponse, PostHotData hotData) {
        return new FanLetterHotResponse(
                baseResponse.fanLetterId(),
                baseResponse.recipientType(),
                baseResponse.recipientArtistMemberId(),
                baseResponse.recipientDisplayName(),
                baseResponse.recipientProfileImageUrl(),
                baseResponse.image(),
                hotData.likeCount(),
                baseResponse.artistLiked(),
                baseResponse.artistLikeDisplayName(),
                baseResponse.artistLikeProfileImageUrl(),
                baseResponse.createdAt()
        );
    }
}
