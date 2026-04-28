package com.example.infinite.domain.artistcontent.media.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ArtistYoutubeVideoImportRequest(
        @Schema(description = "등록할 유튜브 링크", example = "https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        @NotBlank
        @Size(max = 500)
        String youtubeUrl
) {
}
