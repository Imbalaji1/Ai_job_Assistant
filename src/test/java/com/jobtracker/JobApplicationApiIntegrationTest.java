package com.jobtracker;

import com.jobtracker.repository.ApplicationStatusHistoryRepository;
import com.jobtracker.repository.JobApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class JobApplicationApiIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private JobApplicationRepository jobApplicationRepository;

    @Autowired
    private ApplicationStatusHistoryRepository historyRepository;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @BeforeEach
    void clearDatabase() {
        historyRepository.deleteAll();
        jobApplicationRepository.deleteAll();
    }

    @Test
    void createsFiltersUpdatesTracksAndDeletesApplication() throws Exception {
        String createBody = """
                {
                  "companyName": "Example Company",
                  "jobRole": "Java Developer",
                  "salaryLpa": 8.50,
                  "location": "Bengaluru",
                  "status": "Applied",
                  "appliedDate": "2026-11-26"
                }
                """;

        HttpResponse<String> created = send("POST", "/api/applications", createBody);
        assertEquals(201, created.statusCode(), created.body());
        assertTrue(created.body().contains("\"companyName\":\"Example Company\""));
        String location = created.headers().firstValue("Location").orElseThrow();
        String id = location.substring(location.lastIndexOf('/') + 1);

        HttpResponse<String> filtered = send("GET",
                "/api/applications?company=example&status=applied&page=0&size=10", null);
        assertEquals(200, filtered.statusCode(), filtered.body());
        assertTrue(filtered.body().contains("\"totalElements\":1"));
        assertTrue(filtered.body().contains("Example Company"));

        HttpResponse<String> dashboard = send("GET", "/api/dashboard/status-counts", null);
        assertEquals(200, dashboard.statusCode(), dashboard.body());
        assertTrue(dashboard.body().contains("\"totalApplications\":1"));

        HttpResponse<String> updated = send("PATCH", "/api/applications/" + id + "/status",
                "{\"status\":\"Selected\"}");
        assertEquals(200, updated.statusCode(), updated.body());
        assertTrue(updated.body().contains("\"status\":\"Selected\""));

        HttpResponse<String> history = send("GET", "/api/applications/" + id + "/history", null);
        assertEquals(200, history.statusCode(), history.body());
        assertTrue(history.body().contains("\"newStatus\":\"Applied\""));
        assertTrue(history.body().contains("\"previousStatus\":\"Applied\""));
        assertTrue(history.body().contains("\"newStatus\":\"Selected\""));

        HttpResponse<String> deleted = send("DELETE", "/api/applications/" + id, null);
        assertEquals(204, deleted.statusCode());
        assertEquals(404, send("GET", "/api/applications/" + id, null).statusCode());
    }

    @Test
    void rejectsInvalidApplicationInput() throws Exception {
        HttpResponse<String> response = send("POST", "/api/applications", "{\"companyName\":\"\"}");

        assertEquals(400, response.statusCode(), response.body());
        assertTrue(response.body().contains("companyName"));
    }

    private HttpResponse<String> send(String method, String path, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json");
        if (body == null) {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            request.method(method, HttpRequest.BodyPublishers.ofString(body));
        }
        return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
