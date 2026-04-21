package com.example.infinite.domain.dm.error;

import com.example.infinite.global.error.ErrorCodeType;
import lombok.Getter;

@Getter
public class DmException extends RuntimeException {

    private final ErrorCodeType errorCode;

    public DmException(ErrorCodeType errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
