package com.jobtracker.service;

import com.jobtracker.exception.AiServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Component
public class OllamaClient {
    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(3);
    private final String baseUrl;
    private final String model;
    private final JsonMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public OllamaClient(@Value("${app.ollama.base-url:http://localhost:11434}") String baseUrl,
                        @Value("${app.ollama.model:qwen2.5:3b}") String model,
                        JsonMapper objectMapper) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.model = model;
        this.objectMapper = objectMapper;
    }

    public String chat(List<Map<String, String>> messages) {
        try {
            String body = objectMapper.writeValueAsString(
                    Map.of("model", model, "messages", messages, "stream", false));
            HttpRequest request = createRequest(body);
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AiServiceUnavailableException(
                        "Ollama returned HTTP " + response.statusCode() + ".");
            }
            JsonNode content = objectMapper.readTree(response.body())
                    .path("message").path("content");
            if (!content.isTextual() || content.asText().isBlank()) {
                throw new AiServiceUnavailableException(
                        "Ollama returned an empty response. Try again.");
            }
            return content.asText();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw unavailable(exception);
        } catch (IOException exception) {
            throw unavailable(exception);
        } catch (RuntimeException exception) {
            if (exception instanceof AiServiceUnavailableException serviceException) {
                throw serviceException;
            }
            throw unavailable(exception);
        }
    }

    public String stream(List<Map<String, String>> messages, Consumer<String> tokenConsumer) {
        StringBuilder answer = new StringBuilder();
        try {
            String body = objectMapper.writeValueAsString(
                    Map.of("model", model, "messages", messages, "stream", true));
            HttpRequest request = createRequest(body);
            HttpResponse<Stream<String>> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofLines());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                response.body().close();
                throw new AiServiceUnavailableException(
                        "Ollama returned HTTP " + response.statusCode() + ".");
            }
            try (Stream<String> lines = response.body()) {
                var iterator = lines.iterator();
                while (iterator.hasNext()) {
                    String line = iterator.next();
                    if (line.isBlank()) {
                        continue;
                    }
                    JsonNode root = objectMapper.readTree(line);
                    JsonNode content = root.path("message").path("content");
                    if (content.isTextual() && !content.asText().isEmpty()) {
                        String token = content.asText();
                        answer.append(token);
                        tokenConsumer.accept(token);
                    }
                    if (root.path("done").asBoolean(false)) {
                        break;
                    }
                }
            }
            if (answer.isEmpty()) {
                throw new AiServiceUnavailableException(
                        "Ollama returned an empty response. Try again.");
            }
            return answer.toString();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw unavailable(exception);
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof AiServiceUnavailableException serviceException) {
                throw serviceException;
            }
            throw unavailable(exception);
        }
    }

    private AiServiceUnavailableException unavailable(Exception exception) {
        return new AiServiceUnavailableException(
                "The local Ollama service is unavailable. Start Ollama and try again.",
                exception);
    }

    private HttpRequest createRequest(String body) {
        return HttpRequest.newBuilder(URI.create(baseUrl + "/api/chat"))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }
}
