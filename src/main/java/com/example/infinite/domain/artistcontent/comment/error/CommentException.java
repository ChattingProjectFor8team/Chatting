package com.example.infinite.domain.artistcontent.comment.error;

import com.example.infinite.global.error.ErrorCodeType;
import lombok.Getter;

@Getter
public class CommentException extends RuntimeException {

    private final ErrorCodeType errorCode;

    public CommentException(ErrorCodeType errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
