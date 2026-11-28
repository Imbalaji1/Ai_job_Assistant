package com.jobtracker.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RefreshCookieFactory {
    public static final String COOKIE_NAME = "AIJA_REFRESH";
    private final boolean secure;
    private final Duration lifetime;

    public RefreshCookieFactory(
            @Value("${app.security.refresh-cookie-secure:true}") boolean secure,
            @Value("${app.security.refresh-token-ttl:P30D}") Duration lifetime) {
        this.secure = secure;
        this.lifetime = lifetime;
    }

    public ResponseCookie create(String token) {
        return ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(lifetime)
                .build();
    }

    public ResponseCookie clear() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(Duration.ZERO)
                .build();
    }
}
