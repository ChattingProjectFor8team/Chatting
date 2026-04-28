package com.example.infinite.global.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공통 API 응답 포맷")
@JsonInclude(JsonInclude.Include.NON_NULL)
// 모든 API 응답을 success/data/error 구조로 통일하기 위한 래퍼다.
public record ApiResponse<T>(
        @Schema(description = "요청 성공 여부", example = "true")
        boolean success,       // 성공 여부 (true/false)
        @Schema(description = "성공 시 반환되는 실제 데이터")
        T data,               // 성공 시 실제 데이터 (제네릭)
        @Schema(description = "실패 시 반환되는 에러 정보")
        ErrorResponse error   // 실패 시 에러 정보
) {
    // 성공 응답은 data만 담고 error는 비운다.
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    // 예외 처리기에서 이미 만든 ErrorResponse를 그대로 감쌀 때 사용한다.
    public static ApiResponse<Void> fail(ErrorResponse errorResponse) {
        return new ApiResponse<>(false, null, errorResponse);
    }

    // 간단한 실패 응답이 필요할 때 최소 정보만으로 생성한다.
    public static ApiResponse<Void> fail(String code, String message) {
        return new ApiResponse<>(false, null, ErrorResponse.builder()
                .message(message)
                .code(code)
                .build());
    }
}
