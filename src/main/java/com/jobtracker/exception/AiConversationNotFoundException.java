package com.jobtracker.exception;

public class AiConversationNotFoundException extends RuntimeException {
    public AiConversationNotFoundException(Long id) {
        super("AI conversation " + id + " was not found.");
    }
}
