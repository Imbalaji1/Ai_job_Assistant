package com.jobtracker.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResumeTextExtractorTest {
    private final ResumeTextExtractor extractor = new ResumeTextExtractor();

    @Test
    void extractsKnownFieldsFromAnIndependentPdfFixture() throws IOException {
        List<String> expected = List.of(
                "Nila Example", "Java Developer", "Skills: Java, Spring Boot");

        String extracted = extractor.extract(createPdf(expected), ResumeService.PDF_CONTENT_TYPE);

        long matched = expected.stream().filter(extracted::contains).count();
        assertEquals(expected.size(), matched);
    }

    @Test
    void extractsParagraphAndTableTextFromAnIndependentDocxFixture() throws IOException {
        byte[] docx;
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText("Nila Example");
            document.createParagraph().createRun().setText("Java Developer");
            var table = document.createTable();
            table.getRow(0).getCell(0).setText("Skills");
            table.getRow(0).addNewTableCell().setText("Java, Spring Boot");
            document.write(output);
            docx = output.toByteArray();
        }

        String extracted = extractor.extract(docx, ResumeService.DOCX_CONTENT_TYPE);

        assertTrue(extracted.contains("Nila Example"));
        assertTrue(extracted.contains("Java Developer"));
        assertTrue(extracted.contains("Skills | Java, Spring Boot"));
    }

    private byte[] createPdf(List<String> lines) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(50, 750);
                for (String line : lines) {
                    stream.showText(line);
                    stream.newLineAtOffset(0, -18);
                }
                stream.endText();
            }
            document.save(output);
            return output.toByteArray();
        }
    }
}
