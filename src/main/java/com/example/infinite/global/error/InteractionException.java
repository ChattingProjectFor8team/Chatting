package com.example.infinite.global.error;

import lombok.Getter;

@Getter
public class InteractionException extends RuntimeException {

    private final ErrorCode errorCode;

    public InteractionException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
