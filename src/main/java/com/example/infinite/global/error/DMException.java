package com.example.infinite.global.error;

import lombok.Getter;

@Getter
public class DMException extends RuntimeException {

    private final ErrorCode errorCode;

    public DMException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
