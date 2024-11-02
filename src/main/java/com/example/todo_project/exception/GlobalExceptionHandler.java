package com.example.todo_project.exception;

import com.example.todo_project.dto.CommonApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Custom ApplicationException Handler
    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<CommonApiResponse<String>> handleApplicationException(ApplicationException ex) {
        logger.error("Application exception: {}", ex.getMessage());
        CommonApiResponse<String> response = new CommonApiResponse<>(
                ex.getStatus().value(),
                ex.getMessage(),
                null
        );
        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    // Handle validation errors for @Valid annotated requests
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonApiResponse<List<String>>> handleValidationException(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        CommonApiResponse<List<String>> response = new CommonApiResponse<>(
                HttpStatus.BAD_REQUEST.value(),
                "Validation error",
                errors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // Catch-all for unexpected errors
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonApiResponse<String>> handleGeneralException(Exception ex) {
        logger.error("An unexpected error occurred: {}", ex.getMessage(), ex);
        CommonApiResponse<String> response = new CommonApiResponse<>(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An error occurred: " + ex.getMessage(),
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
