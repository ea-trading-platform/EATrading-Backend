package com.eatrading.api.exception;

/**
 * Exception thrown when an authenticated client attempts an unauthorized action.
 * Maps to HTTP 403 Forbidden.
 */
public class ForbiddenOperationException extends RuntimeException {

    public ForbiddenOperationException(String message) {
        super(message);
    }

    public ForbiddenOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}