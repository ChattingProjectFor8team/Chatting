package com.example.infinite.global.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.function.Function;

@Schema(description = "커서 기반 slice 응답")
// Page처럼 전체 개수를 계산하지 않고, 다음 묶음 존재 여부만 판단한다.
public record CursorSliceResponse<T>(
        @Schema(description = "현재 slice 데이터")
        List<T> content,
        @Schema(description = "다음 조회에 사용할 커서", example = "120")
        Long nextCursor,
        @Schema(description = "다음 slice 존재 여부", example = "true")
        boolean hasNext,
        @Schema(description = "이번 요청의 조회 개수", example = "10")
        int size
) {
    public static <T> CursorSliceResponse<T> of(
            List<T> rows,
            int size,
            Function<T, Long> cursorExtractor
    ) {
        // limit + 1 방식으로 한 건 더 조회해 다음 slice 존재 여부를 계산한다.
        boolean hasNext = rows.size() > size;
        List<T> content = hasNext ? rows.subList(0, size) : rows;
        Long nextCursor = content.isEmpty() ? null : cursorExtractor.apply(content.get(content.size() - 1));

        return new CursorSliceResponse<>(content, hasNext ? nextCursor : null, hasNext, size);
    }
}
