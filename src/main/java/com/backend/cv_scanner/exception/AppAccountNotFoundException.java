package com.backend.cv_scanner.exception;

public class AppAccountNotFoundException extends RuntimeException {
    public AppAccountNotFoundException(String message) {
        super(message);
    }
}
