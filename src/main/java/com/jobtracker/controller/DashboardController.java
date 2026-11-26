package com.jobtracker.controller;

import com.jobtracker.dto.StatusCountResponse;
import com.jobtracker.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/status-counts")
    public StatusCountResponse getStatusCounts() {
        return dashboardService.getStatusCounts();
    }
}
