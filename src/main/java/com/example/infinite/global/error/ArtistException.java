package com.example.infinite.global.error;

import lombok.Getter;

@Getter
// 아티스트 계정 또는 권한 관련 예외를 묶는다.
public class ArtistException extends RuntimeException {

    private final ErrorCode errorCode;

    public ArtistException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
