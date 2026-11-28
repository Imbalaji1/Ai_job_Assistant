package com.jobtracker;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.Map;

public final class TestSecurityContext {
    private TestSecurityContext() {
    }

    public static void setAccountId(long accountId) {
        Instant now = Instant.now();
        Jwt jwt = new Jwt("test-token", now, now.plusSeconds(600),
                Map.of("alg", "HS256", "typ", "JWT"),
                Map.of("sub", "test@example.com", "uid", accountId));
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    public static void clear() {
        SecurityContextHolder.clearContext();
    }
}
