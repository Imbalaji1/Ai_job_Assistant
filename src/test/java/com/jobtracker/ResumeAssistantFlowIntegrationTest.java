package com.jobtracker;

import com.jobtracker.dto.AiChatCreateRequest;
import com.jobtracker.dto.RegisterRequest;
import com.jobtracker.dto.JobApplicationRequest;
import com.jobtracker.enums.AiConversationPurpose;
import com.jobtracker.enums.ApplicationStatus;
import com.jobtracker.enums.ResumeKeepChoice;
import com.jobtracker.service.ActivityHistoryService;
import com.jobtracker.service.AuthService;
import com.jobtracker.service.AiChatService;
import com.jobtracker.service.ResumeApplicationService;
import com.jobtracker.service.ResumeDocumentWriter;
import com.jobtracker.service.ResumeService;
import com.jobtracker.service.OllamaClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:resume_assistant_flow_test;MODE=MySQL;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class ResumeAssistantFlowIntegrationTest {
    @Autowired
    private ResumeService resumeService;

    @Autowired
    private ResumeApplicationService resumeApplicationService;

    @Autowired
    private AiChatService aiChatService;

    @Autowired
    private ActivityHistoryService historyService;

    @Autowired
    private ResumeDocumentWriter documentWriter;

    @Autowired
    private AuthService authService;

    @MockitoBean
    private OllamaClient ollamaClient;

    @BeforeEach
    void authenticateTestUser() {
        var user = authService.register(new RegisterRequest(
                "Nila Example", "nila-" + UUID.randomUUID() + "@example.com",
                "long-test-password-123"));
        TestSecurityContext.setAccountId(user.response().user().id());
    }

    @AfterEach
    void clearSecurityContext() {
        TestSecurityContext.clear();
    }

    @Test
    void storesResumeTracksApplicationChatsRevisesAndKeepsBothVersions() {
        String originalText = "Nila Example\nJava Developer\nSkills: Java, Spring Boot";
        byte[] originalDocument = documentWriter.write(
                originalText, ResumeService.DOCX_CONTENT_TYPE);
        var uploadedResume = resumeService.upload(new MockMultipartFile(
                "file", "nila-resume.docx", ResumeService.DOCX_CONTENT_TYPE, originalDocument));

        var application = resumeApplicationService.createApplication(uploadedResume.id(),
                new JobApplicationRequest("Example Company", "Java Developer",
                        new BigDecimal("8.50"), "Bengaluru", ApplicationStatus.APPLIED,
                        LocalDate.of(2026, 11, 27), LocalDate.of(2026, 12, 4)));
        var conversation = aiChatService.create(new AiChatCreateRequest(
                AiConversationPurpose.RESUME_IMPROVEMENT, "Improve my resume",
                uploadedResume.id(), application.id()));
        when(ollamaClient.chat(anyList())).thenReturn(
                "Your resume clearly shows Java experience.",
                "Nila Example\nJava Developer\nSkills: Java and Spring Boot\n"
                        + "Experience: Improved backend services.");

        var reply = aiChatService.sendMessage(conversation.id(), "What should I improve?");
        var revision = aiChatService.generateResumeRevision(
                conversation.id(), "Highlight Java and Spring Boot experience");
        List<String> transcript = aiChatService.getMessages(conversation.id()).stream()
                .map(message -> message.content()).toList();

        assertEquals("Your resume clearly shows Java experience.",
                reply.assistantMessage().content());
        assertEquals("Would you like to keep the original resume, the new resume, or both?",
                revision.question());
        assertTrue(transcript.contains(revision.question()));
        assertEquals(2, resumeService.findAll().size());

        aiChatService.chooseResumeRetention(conversation.id(),
                revision.generatedResume().id(), ResumeKeepChoice.BOTH);

        assertEquals(2, resumeService.findAll().size());
        assertEquals(1, historyService.getSummary().resumeUploads());
        assertEquals(1, historyService.getSummary().aiGeneratedResumeVersions());
        assertEquals(1, historyService.getSummary().aiChatsStarted());
        verify(ollamaClient, org.mockito.Mockito.times(2)).chat(anyList());
    }

    @Test
    void returnsTheExactOutOfScopeMessageWithoutCallingOllama() {
        var conversation = aiChatService.create(new AiChatCreateRequest(
                AiConversationPurpose.CAREER_DEVELOPMENT, "Career chat", null, null));

        var reply = aiChatService.sendMessage(conversation.id(),
                "What is the capital of France?");

        assertEquals("I am built for development purpose only. Sorry, I can't understand your question.",
                reply.assistantMessage().content());
        verify(ollamaClient, org.mockito.Mockito.never()).chat(anyList());

        List<String> streamedTokens = new ArrayList<>();
        aiChatService.streamMessage(conversation.id(), "What is the capital of France?",
                streamedTokens::add);
        assertEquals(List.of(reply.assistantMessage().content()), streamedTokens);
        verify(ollamaClient, org.mockito.Mockito.never()).stream(
                anyList(), org.mockito.ArgumentMatchers.any());
    }
}
