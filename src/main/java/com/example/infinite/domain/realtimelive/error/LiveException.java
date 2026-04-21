package com.example.infinite.domain.realtimelive.error;

import com.example.infinite.global.error.ErrorCodeType;
import lombok.Getter;

@Getter
public class LiveException extends RuntimeException {

    private final ErrorCodeType errorCode;

    public LiveException(ErrorCodeType errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
