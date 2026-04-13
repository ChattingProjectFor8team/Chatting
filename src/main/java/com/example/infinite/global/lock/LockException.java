package com.example.infinite.global.lock;

public class LockException extends RuntimeException {

    public LockException(String message) {
        super(message);
    }

    public LockException(String message, Throwable cause) {
        super(message, cause);
    }
}
