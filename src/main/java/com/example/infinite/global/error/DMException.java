package com.example.infinite.global.error;

import lombok.Getter;

@Getter
// DM 기능에서 사용하는 도메인 전용 예외다.
public class DMException extends RuntimeException {

    private final ErrorCode errorCode;

    public DMException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
