package com.example.infinite.domain.member.artist.error;

import com.example.infinite.global.error.ErrorCodeType;
import lombok.Getter;

@Getter
public class ArtistException extends RuntimeException {

    private final ErrorCodeType errorCode;

    public ArtistException(ErrorCodeType errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
