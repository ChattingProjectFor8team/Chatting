package com.example.infinite.domain.artistcontent.post.fanletter.dto.response;

import com.example.infinite.domain.artistcontent.media.entity.Media;

public record FanLetterImageResponse(
        Long mediaId,
        String imageUrl,
        String thumbnailUrl
) {
    public static FanLetterImageResponse from(Media media) {
        // 팬레터는 이미지 1장만 쓰지만, media 도메인 구조를 재사용하므로
        // 프론트에는 팬레터 전용 응답 형태로 다시 감싸서 내려준다.
        return new FanLetterImageResponse(
                media.getId(),
                media.getFileUrl(),
                media.getThumbnailUrl()
        );
    }
}
