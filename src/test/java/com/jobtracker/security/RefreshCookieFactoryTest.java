package com.jobtracker.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RefreshCookieFactoryTest {
    @Test
    void createsAnHttpOnlySecureStrictCookieScopedToAuthenticationRoutes() {
        RefreshCookieFactory factory = new RefreshCookieFactory(true, Duration.ofDays(30));

        String cookie = factory.create("test-only-refresh-token").toString();

        assertTrue(cookie.startsWith("AIJA_REFRESH="));
        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("Secure"));
        assertTrue(cookie.contains("SameSite=Strict"));
        assertTrue(cookie.contains("Path=/api/auth"));
    }

    @Test
    void clearsTheCookieWithTheSameSecurityAttributes() {
        RefreshCookieFactory factory = new RefreshCookieFactory(true, Duration.ofDays(30));

        String cookie = factory.clear().toString();

        assertTrue(cookie.startsWith("AIJA_REFRESH="));
        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("Secure"));
        assertTrue(cookie.contains("SameSite=Strict"));
        assertTrue(cookie.contains("Path=/api/auth"));
        assertTrue(cookie.contains("Max-Age=0"));
    }
}
