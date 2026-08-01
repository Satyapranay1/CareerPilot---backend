package com.example.careerpilot.exception;

public class InsufficientRoleContextException extends RuntimeException {

    public InsufficientRoleContextException(String message) {
        super(message);
    }

    public InsufficientRoleContextException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}