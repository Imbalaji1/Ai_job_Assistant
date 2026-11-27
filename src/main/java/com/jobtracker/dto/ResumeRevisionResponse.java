package com.jobtracker.dto;

import java.util.List;

public record ResumeRevisionResponse(
        ResumeVersionResponse generatedResume,
        String question,
        List<String> choices,
        String noResponseBehavior) {
}
