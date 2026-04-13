package com.example.infinite.global.error;

import lombok.Getter;

@Getter
public class ArtistException extends RuntimeException {

    private final ErrorCode errorCode;

    public ArtistException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
