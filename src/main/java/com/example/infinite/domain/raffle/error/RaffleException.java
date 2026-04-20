package com.example.infinite.domain.raffle.error;

import com.example.infinite.global.error.ErrorCodeType;
import lombok.Getter;

@Getter
public class RaffleException extends RuntimeException {

    private final ErrorCodeType errorCode;

    public RaffleException(ErrorCodeType errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}