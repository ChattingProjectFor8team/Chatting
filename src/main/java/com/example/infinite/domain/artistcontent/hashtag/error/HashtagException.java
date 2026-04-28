package com.example.infinite.domain.artistcontent.hashtag.error;

import com.example.infinite.global.error.ErrorCodeType;
import lombok.Getter;

@Getter
public class HashtagException extends RuntimeException {

    private final ErrorCodeType errorCode;

    public HashtagException(ErrorCodeType errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
