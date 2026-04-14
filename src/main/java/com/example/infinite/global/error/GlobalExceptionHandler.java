package com.example.infinite.global.error;

import com.example.infinite.global.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import static com.example.infinite.global.error.ErrorCode.INTERNAL_SERVER_ERROR;
import static com.example.infinite.global.error.ErrorCode.INVALID_INPUT_VALUE;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // @Valid 검증 실패 시 발생
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ApiResponse<?>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("handleMethodArgumentNotValidException", e);
        return ResponseEntity
                .status(INVALID_INPUT_VALUE.getStatus())
                .body(ApiResponse.fail(INVALID_INPUT_VALUE.getCode(), INVALID_INPUT_VALUE.getMessage()));
    }

    // 서버 내부에서 발생하는 모든 예외 처리
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResponse<?>> handleException(Exception e) {
        log.error("handleException", e);
        return ResponseEntity
                .status(INTERNAL_SERVER_ERROR.getStatus())
                .body(ApiResponse.fail(INTERNAL_SERVER_ERROR.getCode(), INTERNAL_SERVER_ERROR.getMessage()));
    }
}
