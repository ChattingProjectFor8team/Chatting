package com.example.infinite.global.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "페이징 응답")
// Spring Page 객체를 클라이언트 친화적인 응답 형태로 변환한다.
public record PageResponse<T>(
        @Schema(description = "현재 페이지 데이터")
        List<T> content,
        @Schema(description = "현재 페이지 번호(1부터 시작)", example = "1")
        int number,
        @Schema(description = "페이지 크기", example = "10")
        int size,
        @Schema(description = "전체 페이지 수", example = "5")
        long totalPages,
        @Schema(description = "전체 데이터 수", example = "42")
        long totalElements,
        @Schema(description = "마지막 페이지 여부", example = "false")
        boolean isLast
) {
    public PageResponse(Page<T> page) {
        // Spring 내부 0-based 페이지 번호를 응답에서는 1-based로 맞춘다.
        this(
                page.getContent(),
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.isLast()
        );
    }
}
