package com.jobtracker.service;

import com.jobtracker.exception.TooManyLoginAttemptsException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-email-and-client, in-memory throttling for failed login attempts. The
 * combined identifier is hashed so raw email addresses and client addresses
 * are not retained in this map. A user on another client is not locked out by
 * somebody repeatedly guessing that user's password.
 */
@Component
public class LoginAttemptLimiter {
    private static final int MAX_FAILURES = 10;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private final ConcurrentHashMap<String, AttemptWindow> attempts = new ConcurrentHashMap<>();

    public void checkAllowed(String email, String clientAddress) {
        String key = keyFor(email, clientAddress);
        Instant now = Instant.now();
        AttemptWindow window = attempts.get(key);
        if (window == null) {
            return;
        }
        if (window.blockedUntil() != null && now.isBefore(window.blockedUntil())) {
            throw new TooManyLoginAttemptsException();
        }
        if (!now.isBefore(window.windowEndsAt())) {
            attempts.remove(key, window);
        }
    }

    public void recordFailure(String email, String clientAddress) {
        String key = keyFor(email, clientAddress);
        Instant now = Instant.now();
        attempts.compute(key, (ignored, existing) -> {
            if (existing == null || !now.isBefore(existing.windowEndsAt())) {
                return new AttemptWindow(1, now.plus(WINDOW), null);
            }
            if (existing.blockedUntil() != null && now.isBefore(existing.blockedUntil())) {
                return existing;
            }
            int failures = existing.failures() + 1;
            Instant blockedUntil = failures >= MAX_FAILURES ? now.plus(WINDOW) : null;
            return new AttemptWindow(failures, existing.windowEndsAt(), blockedUntil);
        });
        removeExpiredEntries(now);
    }

    public void recordSuccess(String email, String clientAddress) {
        attempts.remove(keyFor(email, clientAddress));
    }

    private void removeExpiredEntries(Instant now) {
        if (attempts.size() > 4_096) {
            attempts.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().windowEndsAt())
                    && (entry.getValue().blockedUntil() == null
                    || !now.isBefore(entry.getValue().blockedUntil())));
        }
    }

    private String keyFor(String email, String clientAddress) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((email.trim().toLowerCase(Locale.ROOT) + "\u0000" + clientAddress)
                            .getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }

    private record AttemptWindow(int failures, Instant windowEndsAt, Instant blockedUntil) {
    }
}
