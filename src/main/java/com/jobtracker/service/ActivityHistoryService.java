package com.jobtracker.service;

import com.jobtracker.dto.ActivityEventResponse;
import com.jobtracker.dto.HistorySummaryResponse;
import com.jobtracker.entity.ActivityEvent;
import com.jobtracker.repository.ActivityEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ActivityHistoryService {
    private static final int MAX_PAGE_SIZE = 100;
    private final ActivityEventRepository repository;
    private final CurrentAccountProvider currentAccountProvider;

    @Autowired
    public ActivityHistoryService(ActivityEventRepository repository,
                                  CurrentAccountProvider currentAccountProvider) {
        this.repository = repository;
        this.currentAccountProvider = currentAccountProvider;
    }

    public ActivityHistoryService(ActivityEventRepository repository) {
        this(repository, new CurrentAccountProvider());
    }

    @Transactional
    public void record(String eventType, String summary, String entityType, Long entityId) {
        repository.save(new ActivityEvent(eventType, summary, entityType, entityId,
                currentAccountProvider.requireAccountId()));
    }

    public Page<ActivityEventResponse> findEvents(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Page must be zero or greater and size must be from 1 to 100.");
        }
        Pageable pageable = PageRequest.of(page, size);
        return repository.findAllByUserAccountIdOrderByCreatedAtDescIdDesc(
                        currentAccountProvider.requireAccountId(), pageable)
                .map(ActivityEventResponse::from);
    }

    public HistorySummaryResponse getSummary() {
        long accountId = currentAccountProvider.requireAccountId();
        return new HistorySummaryResponse(
                repository.countByUserAccountIdAndEventType(accountId, "RESUME_UPLOADED"),
                repository.countByUserAccountIdAndEventType(accountId, "RESUME_VERSION_GENERATED"),
                repository.countByUserAccountIdAndEventType(accountId, "RESUME_DELETED"),
                repository.countByUserAccountIdAndEventType(accountId, "APPLICATION_CREATED"),
                repository.countByUserAccountIdAndEventType(accountId, "APPLICATION_DELETED"),
                repository.countByUserAccountIdAndEventType(accountId, "AI_CHAT_STARTED"));
    }
}
