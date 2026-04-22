package com.example.infinite.global.s3.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "파일 다운로드 URL 응답")
public record FileDownloadUrlResponse(
        @Schema(description = "발급된 S3 다운로드 URL", example = "https://s3.amazonaws.com/...")
        String url
) {
}