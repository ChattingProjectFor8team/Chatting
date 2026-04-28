package com.example.infinite.domain.artistcontent.post.fanletter.dto.response;

import com.example.infinite.domain.artistcontent.post.cache.PostHotData;
import com.example.infinite.domain.artistcontent.post.fanletter.enums.FanLetterRecipientType;

import java.time.LocalDateTime;

public record FanLetterResponse(
        Long fanLetterId,
        Long artistId,
        Long writerId,
        String writerNickname,
        String writerProfileImageUrl,
        boolean fanMembershipSubscribed,
        boolean dmSubscribed,
        // 프론트에서 "To.아티스트" / "To.아티스트멤버" UI 를 구분하는 기준값이다.
        FanLetterRecipientType recipientType,
        // recipientType=ARTIST_MEMBER 일 때만 내려간다.
        Long recipientArtistMemberId,
        String recipientDisplayName,
        String recipientProfileImageUrl,
        FanLetterImageResponse image,
        long likeCount,
        // 우하단 아티스트 하트 오버레이 노출 여부와 표기 정보를 담는다.
        boolean artistLiked,
        String artistLikeDisplayName,
        String artistLikeProfileImageUrl,
        LocalDateTime createdAt
) {
    public static FanLetterResponse from(FanLetterBaseResponse baseResponse, PostHotData hotData) {
        return new FanLetterResponse(
                baseResponse.fanLetterId(),
                baseResponse.artistId(),
                baseResponse.writerId(),
                baseResponse.writerNickname(),
                baseResponse.writerProfileImageUrl(),
                baseResponse.fanMembershipSubscribed(),
                baseResponse.dmSubscribed(),
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
