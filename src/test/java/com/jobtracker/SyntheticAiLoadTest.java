package com.jobtracker;

import com.jobtracker.service.ResumeDocumentWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayOutputStream;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Opt-in end-to-end load test. It writes only generated profiles into a local,
 * isolated MySQL test schema and leaves the resulting records for Workbench.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "AI_JOB_ASSISTANT_RUN_SYNTHETIC_LOAD", matches = "true")
class SyntheticAiLoadTest {
    private static final int RESUME_COUNT = 60;
    private static final int QUESTION_COUNT = 1_001;
    private static final String REQUEST_HEADER = "X-Requested-With";
    private static final String REQUEST_HEADER_VALUE = "AIJobAssistant";
    private static final String BOUNDARY = "----AIJobAssistantSyntheticLoadBoundary";
    private static final String OFF_TOPIC_REPLY =
            "I am built for development purpose only. Sorry, I can't understand your question.";

    @LocalServerPort
    private int port;

    @Autowired
    private ResumeDocumentWriter documentWriter;

    @Autowired
    private JsonMapper jsonMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private String accessToken;

    @DynamicPropertySource
    static void useOnlyTheSeparateLocalTestSchema(DynamicPropertyRegistry registry) {
        String url = requiredEnvironment("AI_JOB_ASSISTANT_TEST_DB_URL");
        Pattern allowedUrl = Pattern.compile(
                "^jdbc:mysql://(?:localhost|127\\.0\\.0\\.1)(?::\\d+)?/"
                        + "ai_job_assistant_test(?:\\?.*)?$",
                Pattern.CASE_INSENSITIVE);
        if (!allowedUrl.matcher(url).matches()) {
            throw new IllegalStateException("Synthetic load tests may use only the local "
                    + "ai_job_assistant_test MySQL schema.");
        }

        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username",
                () -> requiredEnvironment("AI_JOB_ASSISTANT_TEST_DB_USERNAME"));
        registry.add("spring.datasource.password",
                () -> requiredEnvironment("AI_JOB_ASSISTANT_TEST_DB_PASSWORD"));
        registry.add("spring.datasource.driver-class-name",
                () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("app.security.refresh-cookie-secure", () -> false);
    }

    @Test
    void uploadsSixtyArtificialResumesAndSendsOneThousandAndOneCareerQuestions()
            throws Exception {
        long startedAt = System.nanoTime();
        String email = "synthetic-load-" + UUID.randomUUID() + "@example.com";
        JsonNode registration = postAuthentication("/api/auth/register", Map.of(
                "name", "Synthetic AI Load User",
                "email", email,
                "password", "synthetic-load-test-password-2026"));
        accessToken = registration.path("accessToken").asText();
        assertTrue(!accessToken.isBlank(), "Registration did not return an access token.");

        List<SyntheticResume> resumes = createSyntheticResumes();
        List<Long> conversationIds = new ArrayList<>(RESUME_COUNT);
        for (SyntheticResume resume : resumes) {
            String contentType = resume.number() % 2 == 0
                    ? "application/pdf"
                    : "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            byte[] document = documentWriter.write(resume.documentText(), contentType);
            String extension = resume.number() % 2 == 0 ? "pdf" : "docx";
            JsonNode uploaded = postMultipart("/api/resumes",
                    "synthetic-candidate-%03d.%s".formatted(resume.number(), extension),
                    contentType, document);
            long resumeId = uploaded.path("id").asLong();
            assertTrue(resumeId > 0, "Upload did not return a resume id.");

            JsonNode conversation = postJson("/api/ai/chats", Map.of(
                    "purpose", "RESUME_ANALYSIS",
                    "title", "Synthetic resume analysis %03d".formatted(resume.number()),
                    "resumeVersionId", resumeId), 201);
            long conversationId = conversation.path("id").asLong();
            assertTrue(conversationId > 0, "Chat creation did not return an id.");
            conversationIds.add(conversationId);
        }

        List<String> reportRows = new ArrayList<>(QUESTION_COUNT + 1);
        reportRows.add("question_number,resume_number,category,expected_fact,passed,elapsed_ms,question,answer");
        int factQuestions = 0;
        int factPasses = 0;
        int skillQuestions = 0;
        int skillPasses = 0;
        int roleQuestions = 0;
        int rolePasses = 0;
        int adviceQuestions = 0;
        int adviceUsable = 0;
        long totalAnswerMillis = 0;

        for (int questionNumber = 1; questionNumber <= QUESTION_COUNT; questionNumber++) {
            int resumeIndex = (questionNumber - 1) % RESUME_COUNT;
            SyntheticResume resume = resumes.get(resumeIndex);
            AiQuestion question = questionFor(questionNumber, resume);
            long questionStartedAt = System.nanoTime();
            JsonNode response = postJson("/api/ai/chats/" + conversationIds.get(resumeIndex)
                    + "/messages", Map.of("message", question.text()));
            long elapsedMillis = Duration.ofNanos(System.nanoTime() - questionStartedAt).toMillis();
            String answer = response.path("assistantMessage").path("content").asText("").trim();
            assertTrue(!answer.isBlank(), "Question " + questionNumber + " got an empty answer.");
            totalAnswerMillis += elapsedMillis;

            boolean passed;
            if (question.expectedFact() != null) {
                factQuestions++;
                passed = containsExpectedFacts(answer, question.expectedFact());
                if (passed) factPasses++;
                if (question.category().equals("skill_extraction")) {
                    skillQuestions++;
                    if (passed) skillPasses++;
                }
                if (question.category().equals("job_role_fact")) {
                    roleQuestions++;
                    if (passed) rolePasses++;
                }
            } else {
                adviceQuestions++;
                passed = answer.length() >= 32 && !answer.equals(OFF_TOPIC_REPLY);
                if (passed) adviceUsable++;
            }

            reportRows.add(csvRow(questionNumber, resume.number(), question.category(),
                    question.expectedFact(), passed, elapsedMillis, question.text(), answer));
            if (questionNumber % 25 == 0 || questionNumber == QUESTION_COUNT) {
                System.out.printf(Locale.ROOT,
                        "Synthetic AI load progress: %d/%d questions; factual accuracy %d/%d; "
                                + "average response %.2f sec%n",
                        questionNumber, QUESTION_COUNT, factPasses, factQuestions,
                        totalAnswerMillis / 1_000.0 / questionNumber);
            }
        }

        Path report = Path.of("target", "synthetic-ai-load-report.csv");
        Files.createDirectories(report.getParent());
        Files.write(report, reportRows, StandardCharsets.UTF_8);

        double factAccuracy = factQuestions == 0 ? 0 : 100.0 * factPasses / factQuestions;
        double skillAccuracy = skillQuestions == 0 ? 0 : 100.0 * skillPasses / skillQuestions;
        double roleFactAccuracy = roleQuestions == 0 ? 0 : 100.0 * rolePasses / roleQuestions;
        double usableAdviceRate = adviceQuestions == 0 ? 0 : 100.0 * adviceUsable / adviceQuestions;
        long elapsedSeconds = Duration.ofNanos(System.nanoTime() - startedAt).toSeconds();
        System.out.printf(Locale.ROOT,
                "Synthetic AI load complete: resumes=%d, questions=%d, resume_fact_accuracy=%.1f%%, "
                        + "skill_extraction_accuracy=%.1f%%, job_role_fact_accuracy=%.1f%%, "
                        + "usable_career_or_resume_answers=%.1f%%, elapsed=%d sec, report=%s%n",
                RESUME_COUNT, QUESTION_COUNT, factAccuracy, skillAccuracy, roleFactAccuracy,
                usableAdviceRate,
                elapsedSeconds, report.toAbsolutePath());

        assertEquals(RESUME_COUNT, resumes.size());
        assertEquals(QUESTION_COUNT, reportRows.size() - 1);
        assertTrue(Files.size(report) > 1_000, "The detailed AI report was not written.");
    }

    private JsonNode postAuthentication(String path, Object body) throws Exception {
        String json = jsonMapper.writeValueAsString(body);
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .header(REQUEST_HEADER, REQUEST_HEADER_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return readJson(send(request), 201, path);
    }

    private JsonNode postJson(String path, Object body) throws Exception {
        return postJson(path, body, 200);
    }

    private JsonNode postJson(String path, Object body, int expectedStatus) throws Exception {
        String json = jsonMapper.writeValueAsString(body);
        return authorized("POST", path, json.getBytes(StandardCharsets.UTF_8),
                "application/json", expectedStatus);
    }

    private JsonNode postMultipart(String path, String filename, String contentType,
                                   byte[] content) throws Exception {
        ByteArrayOutputStream body = new ByteArrayOutputStream(content.length + 512);
        write(body, "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\""
                + filename + "\"\r\n"
                + "Content-Type: " + contentType + "\r\n\r\n");
        body.write(content);
        write(body, "\r\n--" + BOUNDARY + "--\r\n");
        return authorized("POST", path, body.toByteArray(),
                "multipart/form-data; boundary=" + BOUNDARY, 201);
    }

    private JsonNode authorized(String method, String path, byte[] body, String contentType)
            throws Exception {
        return authorized(method, path, body, contentType, 200);
    }

    private JsonNode authorized(String method, String path, byte[] body, String contentType,
                                int expectedStatus) throws Exception {
        HttpResponse<String> response = sendAuthorizedOnce(method, path, body, contentType);
        if (response.statusCode() == 401) {
            refreshAccessToken();
            response = sendAuthorizedOnce(method, path, body, contentType);
        }
        return readJson(response, expectedStatus, path);
    }

    private HttpResponse<String> sendAuthorizedOnce(String method, String path, byte[] body,
                                                    String contentType) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path))
                .header("Authorization", "Bearer " + accessToken);
        if (contentType != null) request.header("Content-Type", contentType);
        if (body == null) {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            request.method(method, HttpRequest.BodyPublishers.ofByteArray(body));
        }
        return send(request.build());
    }

    private void refreshAccessToken() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri("/api/auth/refresh"))
                .header(REQUEST_HEADER, REQUEST_HEADER_VALUE)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        JsonNode response = readJson(send(request), 200, "/api/auth/refresh");
        accessToken = response.path("accessToken").asText();
        assertTrue(!accessToken.isBlank(), "Refresh did not return a new access token.");
    }

    private HttpResponse<String> send(HttpRequest request) throws Exception {
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode readJson(HttpResponse<String> response, int expectedStatus, String path)
            throws Exception {
        assertEquals(expectedStatus, response.statusCode(),
                path + " returned " + response.statusCode() + ": " + response.body());
        return jsonMapper.readTree(response.body());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private List<SyntheticResume> createSyntheticResumes() {
        List<String> cities = List.of("Bengaluru", "Hyderabad", "Pune", "Chennai");
        List<String> roles = List.of("Java Backend Developer", "React Frontend Developer",
                "Data Analyst", "DevOps Engineer");
        List<List<String>> skills = List.of(
                List.of("Java", "Spring Boot", "MySQL", "Docker"),
                List.of("JavaScript", "React", "TypeScript", "CSS"),
                List.of("SQL", "Python", "Excel", "Power BI"),
                List.of("Linux", "Docker", "Kubernetes", "AWS"));
        List<String> degrees = List.of("BSc Computer Science", "BCA", "BTech Information Technology");
        List<SyntheticResume> resumes = new ArrayList<>(RESUME_COUNT);
        for (int number = 1; number <= RESUME_COUNT; number++) {
            int profileType = (number - 1) % roles.size();
            resumes.add(new SyntheticResume(number, cities.get((number - 1) % cities.size()),
                    roles.get(profileType), 1 + number % 8, skills.get(profileType),
                    "Synthetic Project " + number, 12 + number % 27,
                    degrees.get((number - 1) % degrees.size()), 2020 + number % 6));
        }
        return resumes;
    }

    private AiQuestion questionFor(int questionNumber, SyntheticResume resume) {
        int category = (questionNumber - 1) % 10;
        int phrasing = ((questionNumber - 1) / 10) % 4;
        String text = switch (category) {
            case 0 -> choose(phrasing,
                    "Which city is listed for this candidate?",
                    "Where is the candidate based, according to the resume?",
                    "Name the city shown in the resume.",
                    "What location does this profile list?");
            case 1 -> choose(phrasing,
                    "What job role does the resume show?",
                    "Name the candidate's listed professional role.",
                    "Which role is this resume focused on?",
                    "What position does the profile describe?");
            case 2 -> choose(phrasing,
                    "How many years of experience are listed?",
                    "State the experience duration from the resume.",
                    "How long has the candidate worked in the listed field?",
                    "What number of experience years appears in this profile?");
            case 3 -> choose(phrasing,
                    "List every technical skill explicitly shown in the resume.",
                    "Which skills appear in the resume's skills section? List them all.",
                    "Give the complete listed skill set without adding qualifications.",
                    "What technical skills does this candidate already list? Include each one.");
            case 4 -> choose(phrasing,
                    "What is the project named in this resume?",
                    "Name the project included in the candidate's experience.",
                    "Which project should the candidate discuss from this profile?",
                    "State the exact synthetic project title shown in the resume.");
            case 5 -> choose(phrasing,
                    "What percentage improvement is stated in the achievement?",
                    "Quote the metric from the candidate's project result.",
                    "What measurable result appears in this resume?",
                    "Give the numeric percentage reported in the achievement.");
            case 6 -> choose(phrasing,
                    "What degree is listed in the education section?",
                    "State the candidate's listed degree.",
                    "Which education qualification appears in the resume?",
                    "Name the degree from the education record.");
            case 7 -> "Suggest one practical next skill for someone pursuing a senior "
                    + resume.role() + " role, based on this resume. Explain briefly.";
            case 8 -> "Rewrite the project achievement as one clear resume bullet. Use only "
                    + "facts from the resume and keep its percentage metric.";
            default -> "What project achievement should this candidate explain in an interview? "
                    + "Use the project and result from the resume.";
        };
        String style = switch (phrasing) {
            case 1 -> " Answer in one sentence.";
            case 2 -> " Be concise and use only the resume facts.";
            case 3 -> " Do not add qualifications that are not listed.";
            default -> "";
        };
        String expected = switch (category) {
            case 0 -> resume.city();
            case 1 -> resume.role();
            case 2 -> resume.years() + " years";
            case 3 -> String.join("|", resume.skills());
            case 4 -> resume.project();
            case 5, 8, 9 -> resume.metric() + "%";
            case 6 -> resume.degree();
            default -> null;
        };
        return new AiQuestion(categoryName(category), text + style, expected);
    }

    private String categoryName(int category) {
        return switch (category) {
            case 0, 2, 4, 5, 6 -> "resume_fact_extraction";
            case 1 -> "job_role_fact";
            case 3 -> "skill_extraction";
            case 7 -> "career_growth_advice";
            case 8 -> "resume_development";
            default -> "interview_preparation";
        };
    }

    private String choose(int index, String... options) {
        return options[index % options.length];
    }

    private boolean containsExpectedFacts(String answer, String expectedFacts) {
        String normalizedAnswer = answer.toLowerCase(Locale.ROOT);
        for (String fact : expectedFacts.split("\\|")) {
            if (!normalizedAnswer.contains(fact.toLowerCase(Locale.ROOT))) return false;
        }
        return true;
    }

    private String csvRow(int questionNumber, int resumeNumber, String category,
                          String expected, boolean passed, long elapsedMillis,
                          String question, String answer) {
        return questionNumber + "," + resumeNumber + "," + csv(category) + ","
                + csv(expected == null ? "" : expected) + "," + passed + ","
                + elapsedMillis + "," + csv(question) + "," + csv(answer);
    }

    private String csv(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private static void write(ByteArrayOutputStream output, String text) throws Exception {
        output.write(text.getBytes(StandardCharsets.UTF_8));
    }

    private static String requiredEnvironment(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Set " + key + " in the local test shell first.");
        }
        return value;
    }

    private record SyntheticResume(int number, String city, String role, int years,
                                   List<String> skills, String project, int metric,
                                   String degree, int graduationYear) {
        String documentText() {
            return "Synthetic Candidate " + number + "\n"
                    + "Location: " + city + "\n"
                    + "Role: " + role + "\n"
                    + "Experience: " + years + " years\n"
                    + "Skills: " + String.join(", ", skills) + "\n"
                    + "Project: " + project + "\n"
                    + "Achievement: Improved project processing by " + metric + "%\n"
                    + "Education: " + degree + ", " + graduationYear + "\n"
                    + "All candidate information is artificial test data.";
        }
    }

    private record AiQuestion(String category, String text, String expectedFact) {
    }
}
