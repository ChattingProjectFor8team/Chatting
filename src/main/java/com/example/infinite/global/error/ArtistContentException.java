package com.example.infinite.global.error;

import lombok.Getter;

@Getter
public class ArtistContentException extends RuntimeException {

    private final ErrorCode errorCode;

    public ArtistContentException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
