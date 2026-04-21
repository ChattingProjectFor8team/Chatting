package com.example.infinite.global.s3.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "파일 업로드 결과 응답")
// 괄호 안에 필드를 선언하면 Getter, 생성자, toString 등이 자동 생성됩니다.
public record FileUploadResponse(
        @Schema(description = "S3에 저장된 파일의 키(경로)", example = "profiles/image.png")
        String key
) {
}
