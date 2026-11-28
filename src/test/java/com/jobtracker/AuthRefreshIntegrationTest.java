package com.jobtracker;

import com.jobtracker.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:auth_refresh_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=none",
        "app.security.jwt.access-token-ttl=PT1S"
})
@ActiveProfiles("test")
class AuthRefreshIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void refreshesExpiredAccessTokenRotatesCookieAndRevokesOnLogout() throws Exception {
        HttpResponse<String> registered = send("POST", "/api/auth/register",
                "{\"name\":\"Refresh User\",\"email\":\"refresh@example.com\","
                        + "\"password\":\"a-long-refresh-password\"}", null);
        assertEquals(201, registered.statusCode(), registered.body());
        String oldAccessToken = extractAccessToken(registered.body());
        String oldRefreshCookie = extractCookie(registered);
        assertTrue(registered.headers().firstValue("Set-Cookie").orElseThrow()
                .contains("HttpOnly"));
        assertTrue(registered.headers().firstValue("Set-Cookie").orElseThrow()
                .contains("SameSite=Strict"));

        Thread.sleep(1_200);
        assertEquals(401, send("GET", "/api/auth/me", null, oldAccessToken).statusCode());

        HttpResponse<String> refreshed = send("POST", "/api/auth/refresh", null,
                oldRefreshCookie);
        assertEquals(200, refreshed.statusCode(), refreshed.body());
        String newAccessToken = extractAccessToken(refreshed.body());
        String newRefreshCookie = extractCookie(refreshed);
        assertNotEquals(oldAccessToken, newAccessToken);
        assertNotEquals(oldRefreshCookie, newRefreshCookie);
        assertEquals(200, send("GET", "/api/auth/me", null, newAccessToken).statusCode());

        String onlyStoredHash = refreshTokenRepository.findAll().getFirst().getTokenHash();
        assertEquals(64, onlyStoredHash.length());
        assertNotEquals(newRefreshCookie.substring(newRefreshCookie.indexOf('=') + 1),
                onlyStoredHash);

        assertEquals(401, send("POST", "/api/auth/refresh", null,
                oldRefreshCookie).statusCode());
        HttpResponse<String> logout = send("POST", "/api/auth/logout", null,
                newRefreshCookie);
        assertEquals(204, logout.statusCode());
        assertTrue(logout.headers().firstValue("Set-Cookie").orElseThrow()
                .contains("Max-Age=0"));
        assertEquals(401, send("POST", "/api/auth/refresh", null,
                newRefreshCookie).statusCode());
    }

    private HttpResponse<String> send(String method, String path, String body,
                                      String tokenOrCookie) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path));
        if (path.startsWith("/api/auth/") && !method.equalsIgnoreCase("GET")) {
            request.header("X-Requested-With", "AIJobAssistant");
        }
        if (tokenOrCookie != null && path.equals("/api/auth/refresh")
                || tokenOrCookie != null && path.equals("/api/auth/logout")) {
            request.header("Cookie", tokenOrCookie);
        } else if (tokenOrCookie != null) {
            request.header("Authorization", "Bearer " + tokenOrCookie);
        }
        if (body != null) {
            request.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body));
        } else {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        }
        return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String extractAccessToken(String body) {
        String marker = "\"accessToken\":\"";
        int start = body.indexOf(marker) + marker.length();
        int end = body.indexOf('"', start);
        return body.substring(start, end);
    }

    private String extractCookie(HttpResponse<?> response) {
        return response.headers().firstValue("Set-Cookie").orElseThrow().split(";", 2)[0];
    }
}
