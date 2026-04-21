package com.example.infinite.domain.artistcontent.post.fanletter.dto.response;

import com.example.infinite.domain.artistcontent.post.fanletter.entity.FanLetter;

public record FanLetterCreateResponse(
        Long fanLetterId
) {
    public static FanLetterCreateResponse from(FanLetter fanLetter) {
        // 생성 직후에는 상세 payload 전체보다 식별자만 돌려주고,
        // 이후 목록/상세 조회에서 카드 렌더링 정보를 조립해 내려준다.
        return new FanLetterCreateResponse(fanLetter.getId());
    }
}
