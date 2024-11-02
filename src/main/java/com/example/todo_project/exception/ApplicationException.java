package com.example.todo_project.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public abstract class ApplicationException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;

    public ApplicationException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    // Custom exceptions extending ApplicationException

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class UserNotFoundException extends ApplicationException {
        public UserNotFoundException(String message) {
            super(message, HttpStatus.NOT_FOUND, "USER_NOT_FOUND");
        }
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class TodoNotFoundException extends ApplicationException {
        public TodoNotFoundException(String message) {
            super(message, HttpStatus.NOT_FOUND, "TODO_NOT_FOUND");
        }
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    public static class TaskAlreadyExistsException extends ApplicationException {
        public TaskAlreadyExistsException(String message) {
            super(message, HttpStatus.CONFLICT, "TASK_ALREADY_EXISTS");
        }
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public static class JwtException extends ApplicationException {
        public JwtException(String message) {
            super(message, HttpStatus.UNAUTHORIZED, "JWT_EXCEPTION");
        }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static class PasswordMismatchException extends ApplicationException {
        public PasswordMismatchException(String message) {
            super(message, HttpStatus.BAD_REQUEST, "PASSWORD_MISMATCH");
        }
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public static class InvalidCredentialsException extends ApplicationException {
        public InvalidCredentialsException(String message) {
            super(message, HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
        }
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    public static class UnauthorizedAccessException extends ApplicationException {
        public UnauthorizedAccessException(String message) {
            super(message, HttpStatus.FORBIDDEN, "UNAUTHORIZED_ACCESS");
        }
    }
}