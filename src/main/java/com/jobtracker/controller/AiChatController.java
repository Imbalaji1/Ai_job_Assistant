package com.jobtracker.controller;

import com.jobtracker.dto.AiChatCreateRequest;
import com.jobtracker.dto.AiConversationResponse;
import com.jobtracker.dto.AiMessagePairResponse;
import com.jobtracker.dto.AiMessageRequest;
import com.jobtracker.dto.AiMessageResponse;
import com.jobtracker.dto.ResumeRevisionRequest;
import com.jobtracker.dto.ResumeRevisionResponse;
import com.jobtracker.dto.ResumeRetentionRequest;
import com.jobtracker.service.AiChatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/ai/chats")
public class AiChatController {
    private final AiChatService chatService;

    public AiChatController(AiChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AiConversationResponse create(@Valid @RequestBody AiChatCreateRequest request) {
        return chatService.create(request);
    }

    @GetMapping
    public List<AiConversationResponse> findAll() {
        return chatService.findAll();
    }

    @GetMapping("/{id}/messages")
    public List<AiMessageResponse> getMessages(@PathVariable("id") Long id) {
        return chatService.getMessages(id);
    }

    @PostMapping("/{id}/messages")
    public AiMessagePairResponse sendMessage(@PathVariable("id") Long id,
                                             @Valid @RequestBody AiMessageRequest request) {
        return chatService.sendMessage(id, request.message());
    }

    @PostMapping(value = "/{id}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessage(@PathVariable("id") Long id,
                                    @Valid @RequestBody AiMessageRequest request) {
        SseEmitter emitter = new SseEmitter(180_000L);
        CompletableFuture.runAsync(() -> {
            try {
                chatService.streamMessage(id, request.message(), token -> sendToken(emitter, token));
                emitter.send(SseEmitter.event().name("complete").data("done"));
                emitter.complete();
            } catch (Exception exception) {
                emitter.completeWithError(exception);
            }
        });
        return emitter;
    }

    @PostMapping("/{id}/resume-revisions")
    public ResumeRevisionResponse generateResumeRevision(
            @PathVariable("id") Long id,
            @Valid @RequestBody ResumeRevisionRequest request) {
        return chatService.generateResumeRevision(id, request.instructions());
    }

    @PostMapping("/{id}/resume-revisions/{generatedVersionId}/keep-choice")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void chooseResumeRetention(
            @PathVariable("id") Long id,
            @PathVariable("generatedVersionId") Long generatedVersionId,
            @Valid @RequestBody ResumeRetentionRequest request) {
        chatService.chooseResumeRetention(id, generatedVersionId, request.keep());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        chatService.delete(id);
    }

    private void sendToken(SseEmitter emitter, String token) {
        try {
            emitter.send(SseEmitter.event().name("token").data(token));
        } catch (IOException exception) {
            throw new IllegalStateException("The chat stream was interrupted.", exception);
        }
    }
}
