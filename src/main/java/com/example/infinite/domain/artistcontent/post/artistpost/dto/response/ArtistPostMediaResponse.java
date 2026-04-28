package com.example.infinite.domain.artistcontent.post.artistpost.dto.response;

import com.example.infinite.domain.artistcontent.media.entity.Media;
import com.example.infinite.domain.artistcontent.media.enums.MediaType;

public record ArtistPostMediaResponse(
        Long mediaId,
        MediaType mediaType,
        String fileUrl,
        String thumbnailUrl,
        Integer sortOrder
) {
    public static ArtistPostMediaResponse from(Media media) {
        // ArtistPost도 Media 공통 엔티티를 그대로 재사용하므로
        // 응답에서는 게시글 렌더링에 필요한 최소 필드만 잘라 내려준다.
        return new ArtistPostMediaResponse(
                media.getId(),
                media.getMediaType(),
                media.getFileUrl(),
                media.getThumbnailUrl(),
                media.getSortOrder()
        );
    }
}
