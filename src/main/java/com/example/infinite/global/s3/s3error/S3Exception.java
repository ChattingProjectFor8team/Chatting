package com.example.infinite.global.s3.s3error;

import lombok.Getter;

@Getter
public class S3Exception extends RuntimeException {
    private final S3ErrorCode errorCode;

    public S3Exception(S3ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}