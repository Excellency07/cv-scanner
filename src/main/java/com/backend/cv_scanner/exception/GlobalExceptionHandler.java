package com.backend.cv_scanner.exception;

import com.backend.cv_scanner.model.dto.ErrorResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDto> handleEmailAlreadyExistsException(RuntimeException exception) {
        ErrorResponseDto responseDto = new ErrorResponseDto(
                Instant.now(),
                HttpStatus.CONFLICT.value(),
                "Conflict",
                exception.getMessage()
        );

        return new ResponseEntity<>(responseDto, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(AppAccountNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleAppAccountNotFoundException(RuntimeException exception) {
        ErrorResponseDto responseDto = new ErrorResponseDto(
                Instant.now(),
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                exception.getMessage()
        );

        return new ResponseEntity<>(responseDto, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidPasswordException(RuntimeException exception) {
        ErrorResponseDto responseDto = new ErrorResponseDto(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                exception.getMessage()
        );

        return new ResponseEntity<>(responseDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(TokenNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleTokenNotFoundException(RuntimeException exception) {
        ErrorResponseDto responseDto = new ErrorResponseDto(
                Instant.now(),
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                exception.getMessage()
        );

        return new ResponseEntity<>(responseDto, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> validationExceptionHandler(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new HashMap<>();

        exception.getBindingResult().getAllErrors()
                .forEach(error -> {
                    String field = ((FieldError) error).getField();
                    String message = error.getDefaultMessage();
                    errors.put(field, message);
                });

        ErrorResponseDto response = new ErrorResponseDto(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                errors
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

}
