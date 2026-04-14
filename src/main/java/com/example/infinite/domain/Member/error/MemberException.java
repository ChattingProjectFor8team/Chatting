package com.example.infinite.domain.Member.error;

import com.example.infinite.global.error.ErrorCodeType;
import lombok.Getter;

@Getter
public class MemberException extends RuntimeException {

    private final ErrorCodeType errorCode;

    public MemberException(ErrorCodeType errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
