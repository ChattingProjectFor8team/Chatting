package com.example.infinite.global.common;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private ErrorResponse error;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<?> fail(String errorCode, String message) {
        return new ApiResponse<>(false, null, new ErrorResponse(errorCode, message));
    }

    @Getter
    @AllArgsConstructor
    private static class ErrorResponse {
        private String code;
        private String message;
    }
}
