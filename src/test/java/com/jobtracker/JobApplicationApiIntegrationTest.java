package com.jobtracker;

import com.jobtracker.repository.ActivityEventRepository;
import com.jobtracker.repository.AiConversationRepository;
import com.jobtracker.repository.AiMessageRepository;
import com.jobtracker.repository.ApplicationResumeSubmissionRepository;
import com.jobtracker.repository.ApplicationStatusHistoryRepository;
import com.jobtracker.repository.JobApplicationRepository;
import com.jobtracker.repository.ResumeVersionRepository;
import com.jobtracker.repository.UserProfileRepository;
import com.jobtracker.service.ResumeDocumentWriter;
import com.jobtracker.service.ResumeService;
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
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Autowired
    private ActivityEventRepository activityEventRepository;

    @Autowired
    private AiConversationRepository aiConversationRepository;

    @Autowired
    private AiMessageRepository aiMessageRepository;

    @Autowired
    private ApplicationResumeSubmissionRepository resumeSubmissionRepository;

    @Autowired
    private ResumeVersionRepository resumeVersionRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private ResumeDocumentWriter resumeDocumentWriter;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @BeforeEach
    void clearDatabase() {
        aiMessageRepository.deleteAll();
        aiConversationRepository.deleteAll();
        resumeSubmissionRepository.deleteAll();
        activityEventRepository.deleteAll();
        userProfileRepository.deleteAll();
        resumeVersionRepository.deleteAll();
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
                  "appliedDate": "2026-11-26",
                  "interviewDate": "2026-12-04"
                }
                """;

        HttpResponse<String> created = send("POST", "/api/applications", createBody);
        assertEquals(201, created.statusCode(), created.body());
        assertTrue(created.body().contains("\"companyName\":\"Example Company\""));
        assertTrue(created.body().contains("\"interviewDate\":\"2026-12-04\""));
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

        HttpResponse<String> changedInterviewDate = send("PATCH",
                "/api/applications/" + id + "/interview-date",
                "{\"interviewDate\":\"2026-12-10\"}");
        assertEquals(200, changedInterviewDate.statusCode(), changedInterviewDate.body());
        assertTrue(changedInterviewDate.body().contains("\"interviewDate\":\"2026-12-10\""));

        HttpResponse<String> clearedInterviewDate = send("PATCH",
                "/api/applications/" + id + "/interview-date", "{\"interviewDate\":null}");
        assertEquals(200, clearedInterviewDate.statusCode(), clearedInterviewDate.body());
        assertTrue(clearedInterviewDate.body().contains("\"interviewDate\":null"));

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

    @Test
    void reportsBadRequestForInvalidFiltersAndNotFoundForMissingApplications() throws Exception {
        assertEquals(400, send("GET", "/api/applications?status=Pending", null).statusCode());
        assertEquals(400, send("GET", "/api/applications?size=101", null).statusCode());
        assertEquals(404, send("GET", "/api/applications/99999", null).statusCode());
    }

    @Test
    void exposesResumeProfileHistoryAndChatRoutes() throws Exception {
        HttpResponse<String> resumes = send("GET", "/api/resumes", null);
        assertEquals(200, resumes.statusCode(), resumes.body());
        assertTrue(resumes.body().contains("[]"));

        HttpResponse<String> savedProfile = send("PUT", "/api/profile",
                "{\"name\":\"Nila Example\",\"email\":\"nila@example.com\"}");
        assertEquals(200, savedProfile.statusCode(), savedProfile.body());
        assertTrue(send("GET", "/api/profile", null).body().contains("nila@example.com"));

        HttpResponse<String> chat = send("POST", "/api/ai/chats",
                "{\"purpose\":\"CAREER_DEVELOPMENT\",\"title\":\"Career plan\"}");
        assertEquals(201, chat.statusCode(), chat.body());
        assertTrue(chat.body().contains("Career plan"));
        assertTrue(send("GET", "/api/ai/chats", null).body().contains("Career plan"));
        int chatIdStart = chat.body().indexOf(':') + 1;
        int chatIdEnd = chat.body().indexOf(',', chatIdStart);
        String chatId = chat.body().substring(chatIdStart, chatIdEnd);
        assertTrue(send("GET", "/api/ai/chats/" + chatId + "/messages", null)
                .body().contains("[]"));

        HttpResponse<String> history = send("GET", "/api/history/summary", null);
        assertEquals(200, history.statusCode(), history.body());
        assertTrue(history.body().contains("\"aiChatsStarted\":1"));
        assertEquals(204, send("DELETE", "/api/ai/chats/" + chatId, null).statusCode());
    }

    @Test
    void resumeUploadApiAcceptsValidDocxAndRejectsMalformedPdf() throws Exception {
        byte[] docx = resumeDocumentWriter.write(
                "Nila Example\nJava Developer", ResumeService.DOCX_CONTENT_TYPE);
        HttpResponse<String> uploaded = sendMultipart("/api/resumes", "file",
                "nila-resume.docx", ResumeService.DOCX_CONTENT_TYPE, docx);

        assertEquals(201, uploaded.statusCode(), uploaded.body());
        assertTrue(uploaded.body().contains("nila-resume.docx"));
        String resumeLocation = uploaded.headers().firstValue("Location").orElseThrow();
        String resumeId = resumeLocation.substring(resumeLocation.lastIndexOf('/') + 1);
        assertTrue(send("GET", "/api/resumes", null).body().contains("nila-resume.docx"));
        HttpResponse<String> download = send("GET", "/api/resumes/" + resumeId + "/download", null);
        assertEquals(200, download.statusCode());
        assertTrue(download.headers().firstValue("Content-Disposition").orElseThrow()
                .contains("nila-resume.docx"));

        HttpResponse<String> application = send("POST", "/api/applications",
                "{\"companyName\":\"Example Company\",\"jobRole\":\"Java Developer\","
                        + "\"salaryLpa\":8.50,\"location\":\"Bengaluru\","
                        + "\"status\":\"Applied\",\"appliedDate\":\"2026-11-27\"}");
        assertEquals(201, application.statusCode(), application.body());
        String applicationLocation = application.headers().firstValue("Location").orElseThrow();
        String applicationId = applicationLocation.substring(applicationLocation.lastIndexOf('/') + 1);
        HttpResponse<String> linked = send("POST",
                "/api/applications/" + applicationId + "/resume-submissions",
                "{\"resumeVersionId\":" + resumeId + "}");
        assertEquals(201, linked.statusCode(), linked.body());
        assertTrue(send("GET", "/api/applications/" + applicationId + "/resume-submissions", null)
                .body().contains("nila-resume.docx"));

        assertEquals(204, send("DELETE", "/api/resumes/" + resumeId, null).statusCode());
        assertFalse(send("GET", "/api/resumes", null).body().contains("nila-resume.docx"));
        assertEquals(404, send("GET", "/api/resumes/" + resumeId + "/download", null).statusCode());

        HttpResponse<String> malformed = sendMultipart("/api/resumes", "file",
                "broken.pdf", ResumeService.PDF_CONTENT_TYPE,
                "%PDF-1.7\nnot a valid pdf".getBytes(StandardCharsets.UTF_8));
        assertEquals(400, malformed.statusCode(), malformed.body());
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

    private HttpResponse<String> sendMultipart(String path, String fieldName,
                                               String filename, String contentType,
                                               byte[] content) throws Exception {
        String boundary = "----AiJobAssistantTestBoundary";
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + fieldName
                + "\"; filename=\"" + filename + "\"\r\n"
                + "Content-Type: " + contentType + "\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8));
        body.write(content);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
