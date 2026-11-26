package com.jobtracker.exception;

public class ApplicationNotFoundException extends RuntimeException {
    public ApplicationNotFoundException(Long id) {
        super("Job application with ID " + id + " was not found.");
    }
}
