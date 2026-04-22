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
        return new ArtistPostMediaResponse(
                media.getId(),
                media.getMediaType(),
                media.getFileUrl(),
                media.getThumbnailUrl(),
                media.getSortOrder()
        );
    }
}
