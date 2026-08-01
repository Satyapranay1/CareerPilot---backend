package com.example.careerpilot.exception;

public class InterviewNotFoundException
        extends RuntimeException {

    public InterviewNotFoundException(Long id) {

        super(
                "Interview not found: " + id
        );
    }
}