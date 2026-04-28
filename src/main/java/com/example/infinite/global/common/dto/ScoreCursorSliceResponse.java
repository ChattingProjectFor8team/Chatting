package com.example.infinite.global.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "점수 + id 복합커서 기반 slice 응답")
// HOT 목록은 id DESC 하나로는 안정적인 페이지네이션이 어렵다.
// 그래서 score DESC, id DESC 정렬과 정확히 대응되는 복합커서를 함께 반환한다.
public record ScoreCursorSliceResponse<T>(
        @Schema(description = "현재 slice 데이터")
        List<T> content,
        @Schema(description = "다음 조회에 사용할 점수 커서", example = "23")
        Long nextScoreCursor,
        @Schema(description = "동점 정렬을 깨기 위한 다음 id 커서", example = "120")
        Long nextIdCursor,
        @Schema(description = "다음 slice 존재 여부", example = "true")
        boolean hasNext,
        @Schema(description = "이번 요청의 조회 개수", example = "10")
        int size
) {
}
