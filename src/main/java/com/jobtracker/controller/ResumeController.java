package com.jobtracker.controller;

import com.jobtracker.dto.JobApplicationRequest;
import com.jobtracker.dto.JobApplicationResponse;
import com.jobtracker.dto.ResumeVersionResponse;
import com.jobtracker.entity.ResumeVersion;
import com.jobtracker.service.ResumeApplicationService;
import com.jobtracker.service.ResumeService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {
    private final ResumeService resumeService;
    private final ResumeApplicationService resumeApplicationService;

    public ResumeController(ResumeService resumeService,
                           ResumeApplicationService resumeApplicationService) {
        this.resumeService = resumeService;
        this.resumeApplicationService = resumeApplicationService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumeVersionResponse> upload(
            @RequestPart("file") MultipartFile file) {
        ResumeVersionResponse uploaded = resumeService.upload(file);
        return ResponseEntity.created(URI.create("/api/resumes/" + uploaded.id()))
                .body(uploaded);
    }

    @GetMapping
    public List<ResumeVersionResponse> findAll() {
        return resumeService.findAll();
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable("id") Long id) {
        ResumeVersion version = resumeService.getActiveVersion(id);
        String disposition = ContentDisposition.attachment()
                .filename(version.getOriginalFilename(), StandardCharsets.UTF_8)
                .build().toString();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(version.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .body(version.getFileData());
    }

    @PostMapping("/{id}/applications")
    public ResponseEntity<JobApplicationResponse> createApplicationForResume(
            @PathVariable("id") Long id,
            @Valid @RequestPart("application") JobApplicationRequest request) {
        JobApplicationResponse created = resumeApplicationService.createApplication(id, request);
        return ResponseEntity.created(URI.create("/api/applications/" + created.id()))
                .body(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        resumeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
