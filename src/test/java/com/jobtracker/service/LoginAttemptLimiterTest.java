package com.jobtracker.service;

import com.jobtracker.exception.TooManyLoginAttemptsException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginAttemptLimiterTest {
    @Test
    void throttlesAnEmailAndClientPairWithoutLockingTheSameAccountForOtherClients() {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter();
        for (int attempt = 0; attempt < 10; attempt++) {
            limiter.recordFailure("user@example.com", "192.0.2.10");
        }

        assertThrows(TooManyLoginAttemptsException.class,
                () -> limiter.checkAllowed("user@example.com", "192.0.2.10"));
        assertDoesNotThrow(() -> limiter.checkAllowed("USER@example.com", "192.0.2.11"));
    }

    @Test
    void successfulLoginClearsOnlyThatClientAttemptWindow() {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter();
        for (int attempt = 0; attempt < 10; attempt++) {
            limiter.recordFailure("user@example.com", "192.0.2.10");
        }
        for (int attempt = 0; attempt < 10; attempt++) {
            limiter.recordFailure("user@example.com", "192.0.2.11");
        }

        limiter.recordSuccess("user@example.com", "192.0.2.10");

        assertDoesNotThrow(() -> limiter.checkAllowed("user@example.com", "192.0.2.10"));
        assertThrows(TooManyLoginAttemptsException.class,
                () -> limiter.checkAllowed("user@example.com", "192.0.2.11"));
    }
}
