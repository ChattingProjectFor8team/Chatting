package com.example.infinite.domain.artistcontent.post.error;

import com.example.infinite.global.error.ErrorCodeType;
import lombok.Getter;

@Getter
public class ArtistContentException extends RuntimeException {

    private final ErrorCodeType errorCode;

    public ArtistContentException(ErrorCodeType errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
