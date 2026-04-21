package com.example.infinite.domain.artistcontent.post.fanletter.dto.response;

import com.example.infinite.domain.artistcontent.post.fanletter.enums.FanLetterRecipientType;

import java.time.LocalDateTime;

// 팬레터 목록/상세 조인 결과를 응답 조립 전 단계에서 받는 projection row 다.
// 수신자 표시용 정보와 special-like 오버레이용 artist 정보만 같이 들고 간다.
public record FanLetterReadRow(
        Long fanLetterId,
        Long artistId,
        Long writerId,
        String writerNickname,
        String writerProfileImageUrl,
        FanLetterRecipientType recipientType,
        Long recipientArtistMemberId,
        String recipientDisplayName,
        String recipientProfileImageUrl,
        String artistDisplayName,
        String artistProfileImageUrl,
        Long likeCount,
        LocalDateTime createdAt
) {
}
