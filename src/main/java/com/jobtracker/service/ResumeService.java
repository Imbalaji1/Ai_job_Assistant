package com.jobtracker.service;

import com.jobtracker.dto.ResumeVersionResponse;
import com.jobtracker.entity.ResumeVersion;
import com.jobtracker.enums.ResumeSource;
import com.jobtracker.exception.ResumeNotFoundException;
import com.jobtracker.repository.ResumeVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class ResumeService {
    public static final String PDF_CONTENT_TYPE = "application/pdf";
    public static final String DOCX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    public static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    private final ResumeVersionRepository resumeRepository;
    private final ActivityHistoryService historyService;
    private final ResumeTextExtractor textExtractor;

    public ResumeService(ResumeVersionRepository resumeRepository,
                         ActivityHistoryService historyService,
                         ResumeTextExtractor textExtractor) {
        this.resumeRepository = resumeRepository;
        this.historyService = historyService;
        this.textExtractor = textExtractor;
    }

    @Transactional
    public ResumeVersionResponse upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Choose a resume file to upload.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Resume files must be 10 MB or smaller.");
        }

        String filename = safeFilename(file.getOriginalFilename());
        String contentType = contentTypeFor(filename);
        try {
            byte[] bytes = file.getBytes();
            validateFileSignature(filename, bytes);
            textExtractor.extract(bytes, contentType);
            ResumeVersion saved = resumeRepository.saveAndFlush(new ResumeVersion(
                    null, filename, contentType, bytes.length, bytes, ResumeSource.UPLOADED));
            historyService.record("RESUME_UPLOADED", "Uploaded resume " + filename,
                    "RESUME", saved.getId());
            return ResumeVersionResponse.from(saved);
        } catch (IOException exception) {
            throw new IllegalArgumentException("The uploaded resume could not be read.", exception);
        }
    }

    public List<ResumeVersionResponse> findAll() {
        return resumeRepository.findAllByDeletedAtIsNullOrderByCreatedAtDescIdDesc()
                .stream().map(ResumeVersionResponse::from).toList();
    }

    public ResumeVersion getActiveVersion(Long id) {
        return resumeRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResumeNotFoundException(id));
    }

    @Transactional
    public void delete(Long id) {
        ResumeVersion version = getActiveVersion(id);
        version.markDeleted(LocalDateTime.now());
        resumeRepository.saveAndFlush(version);
        historyService.record("RESUME_DELETED", "Deleted resume " + version.getOriginalFilename(),
                "RESUME", version.getId());
    }

    @Transactional
    public ResumeVersionResponse saveGeneratedVersion(Long parentVersionId,
                                                       String filename,
                                                       String contentType,
                                                       byte[] content) {
        ResumeVersion parent = getActiveVersion(parentVersionId);
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("Generated resume content cannot be empty.");
        }
        if (content.length > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Generated resumes must be 10 MB or smaller.");
        }
        String safeName = safeFilename(filename);
        String expectedType = contentTypeFor(safeName);
        if (!expectedType.equals(contentType)) {
            throw new IllegalArgumentException("Generated resume format must be PDF or DOCX.");
        }
        validateFileSignature(safeName, content);
        textExtractor.extract(content, expectedType);
        ResumeVersion generated = resumeRepository.saveAndFlush(new ResumeVersion(
                parent, safeName, expectedType, content.length, content,
                ResumeSource.AI_GENERATED));
        historyService.record("RESUME_VERSION_GENERATED",
                "AI created resume version " + safeName, "RESUME", generated.getId());
        return ResumeVersionResponse.from(generated);
    }

    private String safeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("Resume filename is required.");
        }
        String normalized = originalFilename.replace('\\', '/');
        String filename = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        if (filename.isBlank() || filename.length() > 255) {
            throw new IllegalArgumentException("Resume filename must be between 1 and 255 characters.");
        }
        return filename;
    }

    private String contentTypeFor(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) {
            return PDF_CONTENT_TYPE;
        }
        if (lower.endsWith(".docx")) {
            return DOCX_CONTENT_TYPE;
        }
        throw new IllegalArgumentException("Only PDF and DOCX resume files are supported.");
    }

    private void validateFileSignature(String filename, byte[] bytes) {
        if (bytes.length < 4) {
            throw new IllegalArgumentException("The file is too small to be a valid PDF or DOCX.");
        }
        if (filename.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            if (!hasPdfHeader(bytes)) {
                throw new IllegalArgumentException("The uploaded PDF file is invalid.");
            }
        } else if (bytes[0] != 'P' || bytes[1] != 'K') {
            throw new IllegalArgumentException("The uploaded DOCX file is invalid.");
        }
    }

    private boolean hasPdfHeader(byte[] bytes) {
        int lastPossibleOffset = Math.min(bytes.length - 5, 1024);
        for (int offset = 0; offset <= lastPossibleOffset; offset++) {
            if (bytes[offset] == '%' && bytes[offset + 1] == 'P'
                    && bytes[offset + 2] == 'D' && bytes[offset + 3] == 'F'
                    && bytes[offset + 4] == '-') {
                return true;
            }
        }
        return false;
    }
}
