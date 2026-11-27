package com.jobtracker.exception;

public class ResumeNotFoundException extends RuntimeException {
    public ResumeNotFoundException(Long id) {
        super("Resume " + id + " was not found.");
    }
}
