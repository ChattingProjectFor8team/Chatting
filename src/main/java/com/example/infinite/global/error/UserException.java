package com.example.infinite.global.error;

import lombok.Getter;

@Getter
// 일반 사용자 도메인에서 사용하는 예외다.
public class UserException extends RuntimeException {

    private final ErrorCode errorCode;

    public UserException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
