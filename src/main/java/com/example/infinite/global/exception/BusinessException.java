package com.example.infinite.global.exception;

import com.example.infinite.global.error.ErrorCode;
import lombok.Getter;

/**
 * 8팀 프로젝트의 공통 비즈니스 예외 클래스
 * ErrorCode를 필드로 가져서 어떤 에러인지 명확하게 전달합니다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage()); // 부모 생성자에 메시지 전달
        this.errorCode = errorCode;
    }

    public BusinessException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
