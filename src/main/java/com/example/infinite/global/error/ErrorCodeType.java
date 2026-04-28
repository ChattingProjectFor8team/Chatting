package com.example.infinite.global.error;

import org.springframework.http.HttpStatus;

public interface ErrorCodeType {

    HttpStatus getStatus();

    String getCode();

    String getMessage();
}
