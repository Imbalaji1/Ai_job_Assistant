package com.jobtracker.exception;

public class TooManyLoginAttemptsException extends RuntimeException {
    public TooManyLoginAttemptsException() {
        super("Too many failed sign-in attempts. Wait 15 minutes and try again.");
    }
}
