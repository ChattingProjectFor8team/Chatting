package com.example.infinite.global.error;

import lombok.Getter;

@Getter
// 구독/멤버십 기능 전용 예외다.
public class SubscriptionMembershipException extends RuntimeException {

    private final ErrorCode errorCode;

    public SubscriptionMembershipException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
