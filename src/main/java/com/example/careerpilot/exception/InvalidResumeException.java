package com.example.careerpilot.exception;

public class InvalidResumeException extends RuntimeException {

    public InvalidResumeException(String message) {
        super(message);
    }

    public InvalidResumeException(String message, Throwable cause) {
        super(message, cause);
    }
}