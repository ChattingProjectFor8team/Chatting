package com.example.infinite.global.error;

import lombok.Getter;

@Getter
// 실시간 라이브 기능에서 사용하는 예외다.
public class RealtimeLiveException extends RuntimeException {

    private final ErrorCode errorCode;

    public RealtimeLiveException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
