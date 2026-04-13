package com.example.infinite.global.error;

import lombok.Getter;

@Getter
public class RealtimeLiveException extends RuntimeException {

    private final ErrorCode errorCode;

    public RealtimeLiveException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
