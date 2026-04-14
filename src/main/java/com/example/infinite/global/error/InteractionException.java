package com.example.infinite.global.error;

import lombok.Getter;

@Getter
// 커뮤니티 게시글, 댓글, 리액션 관련 예외를 담는다.
public class InteractionException extends RuntimeException {

    private final ErrorCode errorCode;

    public InteractionException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
