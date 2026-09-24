package com.eatrading.backend.exception;

/**
 * Exception thrown when a request is syntactically correct but semantically invalid
 * e.g., business logic validation fails (insufficient funds, invalid state, etc.)
 * Maps to HTTP 422 Unprocessable Entity
 */
public class UnprocessableEntityException extends RuntimeException {
    
    public UnprocessableEntityException(String message) {
        super(message);
    }
    
    public UnprocessableEntityException(String message, Throwable cause) {
        super(message, cause);
    }
}
