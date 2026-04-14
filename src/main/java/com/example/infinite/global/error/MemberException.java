package com.example.infinite.global.error;

import lombok.Getter;

@Getter
// 회원 도메인에서 공통으로 사용하는 예외다.
public class MemberException extends RuntimeException {

    private final ErrorCode errorCode;

    public MemberException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
