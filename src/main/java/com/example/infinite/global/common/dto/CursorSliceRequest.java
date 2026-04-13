package com.example.infinite.global.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "커서 기반 slice 요청")
public record CursorSliceRequest(
        @Schema(description = "다음 조회 시작점 커서", example = "120")
        Long cursor,
        @Schema(description = "한 번에 조회할 데이터 개수", example = "10")
        Integer size
) {
}
