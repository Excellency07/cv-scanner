package com.backend.cv_scanner.exception;

public class CVFileTooLargeException extends RuntimeException {
    public CVFileTooLargeException(String message) {
        super(message);
    }
}
