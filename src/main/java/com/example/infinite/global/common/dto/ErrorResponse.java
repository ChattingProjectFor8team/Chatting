package com.example.infinite.global.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "공통 에러 응답")
// 예외 발생 시 클라이언트에 내려줄 표준 에러 본문이다.
public class ErrorResponse {
    @Schema(description = "에러 발생 시각", example = "2026-04-10T14:30:00")
    private final LocalDateTime timestamp; // 에러가 발생한 시간
    @Schema(description = "HTTP 상태 코드", example = "400")
    private final int status;  // 에러 상태코드
    @Schema(description = "HTTP 에러 이름", example = "BAD_REQUEST")
    private final String error; // 에러 이름
    @Schema(description = "서비스 커스텀 에러 코드", example = "C001")
    private final String code;             // 기획 요구사항 및 클라이언트 식별을 위해 정의한 커스텀 에러 코드 (예: M001)
    @Schema(description = "상세 에러 메시지", example = "요청 값이 유효하지 않습니다.")
    private final String message; // 에러 원인 메시지
    @Schema(description = "에러가 발생한 요청 경로", example = "/api/product/v1/products")
    private final String path;   // 에러가 발생한 경로
}
