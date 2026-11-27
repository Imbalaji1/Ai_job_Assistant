package com.jobtracker.dto;

import com.jobtracker.entity.ActivityEvent;

import java.time.LocalDateTime;

public record ActivityEventResponse(
        Long id,
        String eventType,
        String summary,
        String relatedEntityType,
        Long relatedEntityId,
        LocalDateTime createdAt) {

    public static ActivityEventResponse from(ActivityEvent event) {
        return new ActivityEventResponse(event.getId(), event.getEventType(),
                event.getSummary(), event.getRelatedEntityType(),
                event.getRelatedEntityId(), event.getCreatedAt());
    }
}
