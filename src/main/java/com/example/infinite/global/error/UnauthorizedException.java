package com.example.infinite.global.error;

import lombok.Getter;

@Getter
// 인증이 필요한 요청에 로그인 정보가 없을 때 사용한다.
public class UnauthorizedException extends RuntimeException {

    private final ErrorCodeType errorCode;

    public UnauthorizedException(ErrorCodeType errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
