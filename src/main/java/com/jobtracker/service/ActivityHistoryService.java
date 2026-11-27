package com.jobtracker.service;

import com.jobtracker.dto.ActivityEventResponse;
import com.jobtracker.dto.HistorySummaryResponse;
import com.jobtracker.entity.ActivityEvent;
import com.jobtracker.repository.ActivityEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ActivityHistoryService {
    private static final int MAX_PAGE_SIZE = 100;
    private final ActivityEventRepository repository;

    public ActivityHistoryService(ActivityEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void record(String eventType, String summary, String entityType, Long entityId) {
        repository.save(new ActivityEvent(eventType, summary, entityType, entityId));
    }

    public Page<ActivityEventResponse> findEvents(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Page must be zero or greater and size must be from 1 to 100.");
        }
        Pageable pageable = PageRequest.of(page, size);
        return repository.findAllByOrderByCreatedAtDescIdDesc(pageable)
                .map(ActivityEventResponse::from);
    }

    public HistorySummaryResponse getSummary() {
        return new HistorySummaryResponse(
                repository.countByEventType("RESUME_UPLOADED"),
                repository.countByEventType("RESUME_VERSION_GENERATED"),
                repository.countByEventType("RESUME_DELETED"),
                repository.countByEventType("APPLICATION_CREATED"),
                repository.countByEventType("APPLICATION_DELETED"),
                repository.countByEventType("AI_CHAT_STARTED"));
    }
}
