package com.backend.cv_scanner.exception;

/**
 * The CV content itself cannot be parsed (corrupted, encrypted, scanned image without text...).
 * Retrying will not help, so the batch step skips these immediately.
 */
public class CVParsingException extends RuntimeException {
    public CVParsingException(String message) {
        super(message);
    }

    public CVParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
