package com.example.infinite.global.error;

import lombok.Getter;

@Getter
public class SubscriptionMembershipException extends RuntimeException {

    private final ErrorCode errorCode;

    public SubscriptionMembershipException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
