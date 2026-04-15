package com.example.infinite.domain.artistcontent.interaction.error;

import com.example.infinite.global.error.ErrorCodeType;
import lombok.Getter;

@Getter
public class InteractionException extends RuntimeException {

    private final ErrorCodeType errorCode;

    public InteractionException(ErrorCodeType errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
