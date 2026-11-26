package com.jobtracker.controller;

import com.jobtracker.dto.ApplicationStatusHistoryResponse;
import com.jobtracker.dto.ApplicationStatusUpdateRequest;
import com.jobtracker.dto.JobApplicationRequest;
import com.jobtracker.dto.JobApplicationResponse;
import com.jobtracker.dto.PagedResponse;
import com.jobtracker.service.JobApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class JobApplicationController {
    private final JobApplicationService jobApplicationService;

    public JobApplicationController(JobApplicationService jobApplicationService) {
        this.jobApplicationService = jobApplicationService;
    }

    @PostMapping
    public ResponseEntity<JobApplicationResponse> create(
            @Valid @RequestBody JobApplicationRequest request) {
        JobApplicationResponse created = jobApplicationService.create(request);
        return ResponseEntity.created(URI.create("/api/applications/" + created.id()))
                .body(created);
    }

    @GetMapping
    public PagedResponse<JobApplicationResponse> findAll(
            @RequestParam(name = "company", required = false) String company,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return PagedResponse.from(jobApplicationService.findAll(company, status, page, size));
    }

    @GetMapping("/{id}")
    public JobApplicationResponse findById(@PathVariable("id") Long id) {
        return jobApplicationService.findById(id);
    }

    @PutMapping("/{id}")
    public JobApplicationResponse update(@PathVariable("id") Long id,
                                         @Valid @RequestBody JobApplicationRequest request) {
        return jobApplicationService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public JobApplicationResponse updateStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody ApplicationStatusUpdateRequest request) {
        return jobApplicationService.updateStatus(id, request.status());
    }

    @GetMapping("/{id}/history")
    public List<ApplicationStatusHistoryResponse> getStatusHistory(@PathVariable("id") Long id) {
        return jobApplicationService.getStatusHistory(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        jobApplicationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
