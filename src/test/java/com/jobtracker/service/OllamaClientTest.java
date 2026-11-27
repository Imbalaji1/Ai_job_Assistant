package com.jobtracker.service;

import com.jobtracker.exception.AiServiceUnavailableException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

class OllamaClientTest {
    @Test
    void reportsAUsefulErrorWhenOllamaCannotBeReached() {
        OllamaClient client = new OllamaClient(
                "http://127.0.0.1:1", "qwen2.5:3b", JsonMapper.builder().build());

        assertThrows(AiServiceUnavailableException.class,
                () -> client.chat(List.of(Map.of("role", "user", "content", "Hello"))));
    }
}
