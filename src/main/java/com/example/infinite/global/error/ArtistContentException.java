package com.example.infinite.global.error;

import lombok.Getter;

@Getter
// 아티스트 게시물/콘텐츠 도메인 전용 예외다.
public class ArtistContentException extends RuntimeException {

    private final ErrorCode errorCode;

    public ArtistContentException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
