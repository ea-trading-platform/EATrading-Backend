package com.eatrading.api.exception;

/**
 * Exception thrown when a resource already exists (conflict)
 * Maps to HTTP 409 Conflict
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
