package com.jobtracker.service;

import com.jobtracker.entity.ResumeVersion;
import com.jobtracker.enums.ResumeSource;
import com.jobtracker.repository.ResumeVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeServiceTest {
    @Mock
    private ResumeVersionRepository resumeRepository;

    @Mock
    private ActivityHistoryService historyService;

    @Mock
    private ResumeTextExtractor textExtractor;

    private ResumeService resumeService;

    @BeforeEach
    void setUp() {
        resumeService = new ResumeService(resumeRepository, historyService, textExtractor);
    }

    @Test
    void uploadsAValidPdfAndRecordsItsHistory() throws Exception {
        byte[] pdf = new ResumeDocumentWriter("").write(
                "Nila Example\nJava Developer", ResumeService.PDF_CONTENT_TYPE);
        MockMultipartFile file = new MockMultipartFile(
                "file", "nila-resume.pdf", ResumeService.PDF_CONTENT_TYPE, pdf);
        when(textExtractor.extract(pdf, ResumeService.PDF_CONTENT_TYPE))
                .thenReturn("Nila Example\nJava Developer");
        when(resumeRepository.saveAndFlush(any(ResumeVersion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = resumeService.upload(file);

        assertEquals("nila-resume.pdf", result.originalFilename());
        assertEquals(ResumeSource.UPLOADED, result.source());
        ArgumentCaptor<ResumeVersion> saved = ArgumentCaptor.forClass(ResumeVersion.class);
        verify(resumeRepository).saveAndFlush(saved.capture());
        assertEquals(pdf.length, saved.getValue().getFileSize());
        verify(historyService).record(eq("RESUME_UPLOADED"), any(), eq("RESUME"), any());
    }

    @Test
    void rejectsMalformedPdfBeforeSavingIt() {
        byte[] malformedPdf = "%PDF-1.7\nnot a document".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "broken.pdf", ResumeService.PDF_CONTENT_TYPE, malformedPdf);
        when(textExtractor.extract(malformedPdf, ResumeService.PDF_CONTENT_TYPE))
                .thenThrow(new IllegalArgumentException("Invalid PDF"));

        assertThrows(IllegalArgumentException.class, () -> resumeService.upload(file));

        verify(resumeRepository, never()).saveAndFlush(any(ResumeVersion.class));
        verify(historyService, never()).record(any(), any(), any(), any());
    }

    @Test
    void rejectsUnsupportedFileExtensions() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.txt", "text/plain", "resume".getBytes());

        assertThrows(IllegalArgumentException.class, () -> resumeService.upload(file));

        verify(resumeRepository, never()).saveAndFlush(any(ResumeVersion.class));
    }
}
