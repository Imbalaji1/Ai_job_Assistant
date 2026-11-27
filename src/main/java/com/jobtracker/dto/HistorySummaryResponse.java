package com.jobtracker.dto;

public record HistorySummaryResponse(
        long resumeUploads,
        long aiGeneratedResumeVersions,
        long resumeDeletions,
        long applicationCreations,
        long applicationDeletions,
        long aiChatsStarted) {
}
