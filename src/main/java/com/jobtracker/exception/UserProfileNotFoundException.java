package com.jobtracker.exception;

public class UserProfileNotFoundException extends RuntimeException {
    public UserProfileNotFoundException() {
        super("Profile has not been created yet.");
    }
}
