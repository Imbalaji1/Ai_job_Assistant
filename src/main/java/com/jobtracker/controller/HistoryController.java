package com.jobtracker.controller;

import com.jobtracker.dto.ActivityEventResponse;
import com.jobtracker.dto.HistorySummaryResponse;
import com.jobtracker.dto.PagedResponse;
import com.jobtracker.service.ActivityHistoryService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/history")
public class HistoryController {
    private final ActivityHistoryService historyService;

    public HistoryController(ActivityHistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping
    public PagedResponse<ActivityEventResponse> findEvents(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        Page<ActivityEventResponse> events = historyService.findEvents(page, size);
        return PagedResponse.from(events);
    }

    @GetMapping("/summary")
    public HistorySummaryResponse getSummary() {
        return historyService.getSummary();
    }
}
