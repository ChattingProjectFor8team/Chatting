package com.example.infinite.global.error;

import lombok.Getter;

@Getter
public class RaffleException extends RuntimeException {

    private final ErrorCode errorCode;

    public RaffleException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
