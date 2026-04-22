package com.example.infinite.domain.artistcontent.post.fanletter.dto.response;

import com.example.infinite.domain.artistcontent.post.fanletter.enums.FanLetterRecipientType;

import java.time.LocalDateTime;

// 팬레터 목록 카드에 필요한 최소 조인 결과만 담는 projection row 다.
// 목록에서는 작성자/배지 대신 수신자 정보와 special-like용 artist 정보만 사용한다.
public record FanLetterListRow(
        Long fanLetterId,
        FanLetterRecipientType recipientType,
        Long recipientArtistMemberId,
        String recipientDisplayName,
        String recipientProfileImageUrl,
        String artistDisplayName,
        String artistProfileImageUrl,
        LocalDateTime createdAt
) {
}
