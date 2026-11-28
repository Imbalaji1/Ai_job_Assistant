package com.jobtracker;

import com.jobtracker.repository.ActivityEventRepository;
import com.jobtracker.repository.AiConversationRepository;
import com.jobtracker.repository.AiMessageRepository;
import com.jobtracker.repository.ApplicationResumeSubmissionRepository;
import com.jobtracker.repository.ApplicationStatusHistoryRepository;
import com.jobtracker.repository.JobApplicationRepository;
import com.jobtracker.repository.ResumeVersionRepository;
import com.jobtracker.repository.UserProfileRepository;
import com.jobtracker.repository.UserAccountRepository;
import com.jobtracker.repository.RefreshTokenRepository;
import com.jobtracker.entity.JobApplication;
import com.jobtracker.enums.ApplicationStatus;
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
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private ResumeDocumentWriter resumeDocumentWriter;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private String accessToken;

    @BeforeEach
    void clearDatabase() throws Exception {
        aiMessageRepository.deleteAll();
        aiConversationRepository.deleteAll();
        resumeSubmissionRepository.deleteAll();
        activityEventRepository.deleteAll();
        userProfileRepository.deleteAll();
        resumeVersionRepository.deleteAll();
        historyRepository.deleteAll();
        jobApplicationRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userAccountRepository.deleteAll();

        HttpResponse<String> registration = sendUnauthenticated("POST", "/api/auth/register",
                "{\"name\":\"Nila Example\",\"email\":\"nila@example.com\","
                        + "\"password\":\"correct-horse-battery-staple\"}");
        assertEquals(201, registration.statusCode(), registration.body());
        accessToken = extractAccessToken(registration.body());
    }

    @Test
    void requiresAuthenticationAndSupportsRegistrationLoginAndCurrentUser() throws Exception {
        assertEquals(401, sendUnauthenticated("GET", "/api/applications", null).statusCode());
        assertEquals(401, sendWithToken("not-a-valid-token", "GET", "/api/applications", null)
                .statusCode());

        HttpResponse<String> login = sendUnauthenticated("POST", "/api/auth/login",
                "{\"email\":\"NILA@example.com\","
                        + "\"password\":\"correct-horse-battery-staple\"}");
        assertEquals(200, login.statusCode(), login.body());
        String loginToken = extractAccessToken(login.body());
        HttpResponse<String> currentUser = sendWithToken(loginToken, "GET", "/api/auth/me", null);
        assertEquals(200, currentUser.statusCode(), currentUser.body());
        assertTrue(currentUser.body().contains("nila@example.com"));
        String passwordHash = userAccountRepository.findByEmailIgnoreCase("nila@example.com")
                .orElseThrow().getPasswordHash();
        assertEquals(60, passwordHash.length());
        assertFalse(passwordHash.contains("correct-horse-battery-staple"));

        HttpResponse<String> wrongPassword = sendUnauthenticated("POST", "/api/auth/login",
                "{\"email\":\"nila@example.com\",\"password\":\"wrong-password\"}");
        assertEquals(401, wrongPassword.statusCode(), wrongPassword.body());

        HttpResponse<String> duplicateEmail = sendUnauthenticated("POST", "/api/auth/register",
                "{\"name\":\"Another User\",\"email\":\"NILA@example.com\","
                        + "\"password\":\"another-long-password-123\"}");
        assertEquals(409, duplicateEmail.statusCode(), duplicateEmail.body());
    }

    @Test
    void protectsAuthenticationRequestsAndAddsSecurityHeaders() throws Exception {
        HttpResponse<String> missingHeader = sendAuthRequest("/api/auth/register",
                "{\"name\":\"Blocked User\",\"email\":\"blocked@example.com\","
                        + "\"password\":\"a-long-blocked-password\"}", null, false);
        assertEquals(403, missingHeader.statusCode(), missingHeader.body());

        HttpResponse<String> crossSite = sendAuthRequest("/api/auth/register",
                "{\"name\":\"Cross Site\",\"email\":\"cross@example.com\","
                        + "\"password\":\"a-long-cross-site-password\"}", "cross-site", true);
        assertEquals(403, crossSite.statusCode(), crossSite.body());

        HttpResponse<String> allowed = sendUnauthenticated("POST", "/api/auth/register",
                "{\"name\":\"Header User\",\"email\":\"header@example.com\","
                        + "\"password\":\"a-long-header-password\"}");
        assertEquals(201, allowed.statusCode(), allowed.body());
        assertEquals("DENY", allowed.headers().firstValue("X-Frame-Options").orElseThrow());
        assertEquals("nosniff", allowed.headers()
                .firstValue("X-Content-Type-Options").orElseThrow());
        assertEquals("no-referrer", allowed.headers()
                .firstValue("Referrer-Policy").orElseThrow());
        assertEquals("camera=(), microphone=(), geolocation=()", allowed.headers()
                .firstValue("Permissions-Policy").orElseThrow());
        assertTrue(allowed.headers().firstValue("Content-Security-Policy")
                .orElseThrow().contains("frame-ancestors 'none'"));
    }

    @Test
    void limitsRepeatedFailedLoginsAndReturnsSameErrorForUnknownAccounts() throws Exception {
        HttpResponse<String> knownAccountFailure = sendUnauthenticated("POST", "/api/auth/login",
                "{\"email\":\"nila@example.com\",\"password\":\"wrong-password\"}");
        HttpResponse<String> unknownAccountFailure = sendUnauthenticated("POST", "/api/auth/login",
                "{\"email\":\"nobody@example.com\",\"password\":\"wrong-password\"}");
        assertEquals(knownAccountFailure.statusCode(), unknownAccountFailure.statusCode());
        assertTrue(knownAccountFailure.body().contains("Email or password is incorrect."));
        assertTrue(unknownAccountFailure.body().contains("Email or password is incorrect."));

        for (int attempt = 1; attempt < 10; attempt++) {
            assertEquals(401, sendUnauthenticated("POST", "/api/auth/login",
                    "{\"email\":\"throttle@example.com\","
                            + "\"password\":\"wrong-password\"}").statusCode());
        }
        HttpResponse<String> blocked = sendUnauthenticated("POST", "/api/auth/login",
                "{\"email\":\"throttle@example.com\",\"password\":\"wrong-password\"}");
        assertEquals(401, blocked.statusCode());
        HttpResponse<String> rateLimited = sendUnauthenticated("POST", "/api/auth/login",
                "{\"email\":\"throttle@example.com\",\"password\":\"wrong-password\"}");
        assertEquals(429, rateLimited.statusCode(), rateLimited.body());
        assertEquals("900", rateLimited.headers().firstValue("Retry-After").orElseThrow());
    }

    @Test
    void accountsCannotReadOrChangeAnotherAccountsApplications() throws Exception {
        HttpResponse<String> created = send("POST", "/api/applications",
                "{\"companyName\":\"Private Company\",\"jobRole\":\"Java Developer\","
                        + "\"salaryLpa\":8.50,\"location\":\"Bengaluru\","
                        + "\"status\":\"Applied\",\"appliedDate\":\"2026-11-27\"}");
        assertEquals(201, created.statusCode(), created.body());
        String location = created.headers().firstValue("Location").orElseThrow();
        String applicationId = location.substring(location.lastIndexOf('/') + 1);

        byte[] docx = resumeDocumentWriter.write("Nila Example\nJava Developer",
                ResumeService.DOCX_CONTENT_TYPE);
        HttpResponse<String> uploaded = sendMultipart("/api/resumes", "file", "private.docx",
                ResumeService.DOCX_CONTENT_TYPE, docx);
        assertEquals(201, uploaded.statusCode(), uploaded.body());
        String resumeLocation = uploaded.headers().firstValue("Location").orElseThrow();
        String resumeId = resumeLocation.substring(resumeLocation.lastIndexOf('/') + 1);

        HttpResponse<String> createdChat = send("POST", "/api/ai/chats",
                "{\"purpose\":\"CAREER_DEVELOPMENT\",\"title\":\"Private career chat\"}");
        assertEquals(201, createdChat.statusCode(), createdChat.body());
        int chatIdStart = createdChat.body().indexOf(':') + 1;
        int chatIdEnd = createdChat.body().indexOf(',', chatIdStart);
        String chatId = createdChat.body().substring(chatIdStart, chatIdEnd);

        HttpResponse<String> secondRegistration = sendUnauthenticated("POST", "/api/auth/register",
                "{\"name\":\"Second User\",\"email\":\"second@example.com\","
                        + "\"password\":\"another-long-password-123\"}");
        assertEquals(201, secondRegistration.statusCode(), secondRegistration.body());
        String secondToken = extractAccessToken(secondRegistration.body());

        HttpResponse<String> secondUsersApplications = sendWithToken(secondToken, "GET",
                "/api/applications", null);
        assertEquals(200, secondUsersApplications.statusCode(), secondUsersApplications.body());
        assertTrue(secondUsersApplications.body().contains("\"totalElements\":0"));
        assertTrue(sendWithToken(secondToken, "GET", "/api/resumes", null)
                .body().contains("[]"));
        assertEquals(404, sendWithToken(secondToken, "GET",
                "/api/resumes/" + resumeId + "/download", null).statusCode());
        assertTrue(sendWithToken(secondToken, "GET", "/api/ai/chats", null)
                .body().contains("[]"));
        assertEquals(404, sendWithToken(secondToken, "GET",
                "/api/ai/chats/" + chatId + "/messages", null).statusCode());
        assertEquals(404, sendWithToken(secondToken, "DELETE",
                "/api/ai/chats/" + chatId, null).statusCode());
        assertEquals(404, sendWithToken(secondToken, "DELETE",
                "/api/resumes/" + resumeId, null).statusCode());
        String secondProfile = sendWithToken(secondToken, "GET", "/api/profile", null).body();
        assertTrue(secondProfile.contains("second@example.com"));
        assertFalse(secondProfile.contains("nila@example.com"));
        String secondHistory = sendWithToken(secondToken, "GET", "/api/history", null).body();
        assertTrue(secondHistory.contains("\"totalElements\":0"));
        assertFalse(secondHistory.contains("Private Company"));
        assertEquals(404, sendWithToken(secondToken, "GET",
                "/api/applications/" + applicationId, null).statusCode());
        assertEquals(404, sendWithToken(secondToken, "DELETE",
                "/api/applications/" + applicationId, null).statusCode());
    }

    @Test
    void firstRegisteredAccountReceivesExistingUnownedRecords() throws Exception {
        userProfileRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userAccountRepository.deleteAll();
        JobApplication legacyApplication = new JobApplication("Legacy Company", "Developer",
                new java.math.BigDecimal("5.00"), "Remote", ApplicationStatus.APPLIED,
                java.time.LocalDate.of(2026, 11, 27));
        jobApplicationRepository.saveAndFlush(legacyApplication);

        HttpResponse<String> firstRegistration = sendUnauthenticated("POST", "/api/auth/register",
                "{\"name\":\"Legacy Owner\",\"email\":\"legacy@example.com\","
                        + "\"password\":\"legacy-account-password-123\"}");
        assertEquals(201, firstRegistration.statusCode(), firstRegistration.body());
        String legacyOwnerToken = extractAccessToken(firstRegistration.body());
        HttpResponse<String> migratedApplications = sendWithToken(legacyOwnerToken, "GET",
                "/api/applications", null);
        assertEquals(200, migratedApplications.statusCode(), migratedApplications.body());
        assertTrue(migratedApplications.body().contains("Legacy Company"));
        assertTrue(migratedApplications.body().contains("\"totalElements\":1"));
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
    void rejectsOversizedResumeAndRemovesPathFromUploadedFilename() throws Exception {
        byte[] validDocx = resumeDocumentWriter.write("Synthetic Resume\nJava Developer",
                ResumeService.DOCX_CONTENT_TYPE);
        HttpResponse<String> traversalName = sendMultipart("/api/resumes", "file",
                "..\\..\\private-resume.docx", ResumeService.DOCX_CONTENT_TYPE, validDocx);
        assertEquals(201, traversalName.statusCode(), traversalName.body());
        assertTrue(traversalName.body().contains("private-resume.docx"));
        assertFalse(traversalName.body().contains("..\\"));
        assertFalse(traversalName.body().contains("../"));

        byte[] oversizedContent = new byte[(int) ResumeService.MAX_FILE_SIZE + 1];
        HttpResponse<String> oversized = sendMultipart("/api/resumes", "file", "too-big.pdf",
                ResumeService.PDF_CONTENT_TYPE, oversizedContent);
        assertEquals(413, oversized.statusCode(), oversized.body());
        assertTrue(oversized.body().contains("10 MB or smaller"));
    }

    @Test
    void storesSixtySyntheticPdfAndDocxResumesThroughTheUploadApi() throws Exception {
        for (int number = 1; number <= 60; number++) {
            String text = "Synthetic Candidate " + number + "\n"
                    + "Java Backend Developer\n"
                    + "Skills: Java, Spring Boot, MySQL\n"
                    + "Experience: 3 years\n"
                    + "This is artificial test data.";
            boolean usePdf = number % 2 == 0;
            String extension = usePdf ? "pdf" : "docx";
            String contentType = usePdf
                    ? ResumeService.PDF_CONTENT_TYPE : ResumeService.DOCX_CONTENT_TYPE;
            byte[] file = resumeDocumentWriter.write(text, contentType);

            HttpResponse<String> uploaded = sendMultipart("/api/resumes", "file",
                    "synthetic-candidate-" + number + "." + extension, contentType, file);
            assertEquals(201, uploaded.statusCode(),
                    "Resume " + number + " was not stored: " + uploaded.body());
        }

        HttpResponse<String> storedResumes = send("GET", "/api/resumes", null);
        assertEquals(200, storedResumes.statusCode(), storedResumes.body());
        assertEquals(60, storedResumes.body().split("\\\"originalFilename\\\"").length - 1);
        HttpResponse<String> history = send("GET", "/api/history/summary", null);
        assertTrue(history.body().contains("\"resumeUploads\":60"), history.body());
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
        return sendWithToken(accessToken, method, path, body);
    }

    private HttpResponse<String> sendUnauthenticated(String method, String path, String body)
            throws Exception {
        return sendWithToken(null, method, path, body);
    }

    private HttpResponse<String> sendWithToken(String token, String method, String path,
                                               String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json");
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        if (path.startsWith("/api/auth/") && !method.equalsIgnoreCase("GET")) {
            request.header("X-Requested-With", "AIJobAssistant");
        }
        if (body == null) {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            request.method(method, HttpRequest.BodyPublishers.ofString(body));
        }
        return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendAuthRequest(String path, String body, String fetchSite,
                                                 boolean includeApplicationHeader)
            throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path));
        if (body != null) {
            request.header("Content-Type", "application/json");
        }
        if (includeApplicationHeader) {
            request.header("X-Requested-With", "AIJobAssistant");
        }
        if (fetchSite != null) {
            request.header("Sec-Fetch-Site", fetchSite);
        }
        return httpClient.send(request.POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
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
                .header("Authorization", "Bearer " + accessToken)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String extractAccessToken(String body) {
        String marker = "\"accessToken\":\"";
        int start = body.indexOf(marker);
        if (start < 0) {
            throw new IllegalStateException("Authentication response did not include an access token.");
        }
        start += marker.length();
        int end = body.indexOf('"', start);
        return body.substring(start, end);
    }
}
