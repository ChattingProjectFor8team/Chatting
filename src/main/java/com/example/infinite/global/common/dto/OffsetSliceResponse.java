package com.example.infinite.global.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "오프셋 기반 slice 응답")
// 전체 개수(total)를 세지 않고 "다음 묶음이 더 있는가"만 전달하는 가벼운 응답이다.
// HOT처럼 후보군이 이미 많이 줄어든 목록에서 Page보다 부담을 낮추기 위해 사용한다.
public record OffsetSliceResponse<T>(
        @Schema(description = "현재 slice 데이터")
        List<T> content,
        @Schema(description = "다음 조회에 사용할 offset", example = "10")
        Integer nextOffset,
        @Schema(description = "다음 slice 존재 여부", example = "true")
        boolean hasNext,
        @Schema(description = "이번 요청의 조회 개수", example = "10")
        int size
) {
    public static <T> OffsetSliceResponse<T> of(List<T> rows, int offset, int size) {
        // limit + 1 방식으로 한 건 더 읽고 다음 offset 존재 여부만 계산한다.
        boolean hasNext = rows.size() > size;
        List<T> content = hasNext ? rows.subList(0, size) : rows;
        Integer nextOffset = hasNext ? offset + size : null;

        return new OffsetSliceResponse<>(content, nextOffset, hasNext, size);
    }
}
