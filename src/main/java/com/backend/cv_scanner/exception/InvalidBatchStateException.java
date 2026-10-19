package com.backend.cv_scanner.exception;

public class InvalidBatchStateException extends RuntimeException {
    public InvalidBatchStateException(String message) {
        super(message);
    }
}
