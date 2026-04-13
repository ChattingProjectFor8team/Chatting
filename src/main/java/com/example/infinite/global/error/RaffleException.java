package com.example.infinite.global.error;

import lombok.Getter;

@Getter
// 래플 이벤트 기능 전용 예외다.
public class RaffleException extends RuntimeException {

    private final ErrorCode errorCode;

    public RaffleException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
