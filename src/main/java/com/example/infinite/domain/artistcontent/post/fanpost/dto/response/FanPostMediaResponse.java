package com.example.infinite.domain.artistcontent.post.fanpost.dto.response;

import com.example.infinite.domain.artistcontent.media.entity.Media;
import com.example.infinite.domain.artistcontent.media.enums.MediaType;

public record FanPostMediaResponse(
        Long mediaId,
        MediaType mediaType,
        String fileUrl,
        String thumbnailUrl,
        Integer sortOrder
) {
    public static FanPostMediaResponse from(Media media) {
        // media 공통 엔티티를 팬 게시글 응답용 최소 표현으로 줄여서 반환한다.
        return new FanPostMediaResponse(
                media.getId(),
                media.getMediaType(),
                media.getFileUrl(),
                media.getThumbnailUrl(),
                media.getSortOrder()
        );
    }
}
