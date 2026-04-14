package com.example.infinite.global.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "페이지 번호 기반 페이징 응답")
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
