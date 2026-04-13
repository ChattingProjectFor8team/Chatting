package com.example.infinite.global.error;

import lombok.Getter;

@Getter
// 결제/재화 도메인에서 사용하는 예외다.
public class PaymentException extends RuntimeException {

    private final ErrorCode errorCode;

    public PaymentException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
