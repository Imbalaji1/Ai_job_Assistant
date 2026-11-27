package com.jobtracker.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResumeDocumentWriterTest {
    private final ResumeDocumentWriter writer = new ResumeDocumentWriter("");
    private final ResumeTextExtractor textExtractor = new ResumeTextExtractor();

    @Test
    void generatedDocxRetainsExpectedResumeText() {
        String expected = "Nila Example\nJava Developer\nSkills: Java, Spring Boot\nExperience: 2022-2026";

        byte[] docx = writer.write(expected, ResumeService.DOCX_CONTENT_TYPE);
        String extracted = textExtractor.extract(docx, ResumeService.DOCX_CONTENT_TYPE);

        assertTrue(extracted.contains("Nila Example"));
        assertTrue(extracted.contains("Skills: Java, Spring Boot"));
        assertTrue(extracted.contains("Experience: 2022-2026"));
    }

    @Test
    void generatedPdfKeepsAllLinesAcrossPages() throws IOException {
        String expected = IntStream.rangeClosed(1, 75)
                .mapToObj(number -> "Resume line " + number + " - Java and Spring experience")
                .collect(Collectors.joining("\n"));

        byte[] pdf = writer.write(expected, ResumeService.PDF_CONTENT_TYPE);
        String extracted = textExtractor.extract(pdf, ResumeService.PDF_CONTENT_TYPE);
        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertTrue(document.getNumberOfPages() > 1);
        }

        assertTrue(extracted.contains("Resume line 1"));
        assertTrue(extracted.contains("Resume line 75"));
        assertEquals(75, extracted.lines().filter(line -> line.startsWith("Resume line ")).count());
    }

    @Test
    void rejectsEmptyResumeText() {
        assertThrows(IllegalArgumentException.class,
                () -> writer.write("  ", ResumeService.DOCX_CONTENT_TYPE));
    }

    @Test
    void doesNotSilentlyReplaceCharactersMissingFromDefaultPdfFont() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> writer.write("Resume skills: 🧭", ResumeService.PDF_CONTENT_TYPE));

        assertTrue(error.getMessage().contains("RESUME_PDF_FONT_PATH"));
    }
}
