package com.jobtracker.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class ResumeTextExtractor {
    public String extract(byte[] fileData, String contentType) {
        try {
            if (ResumeService.PDF_CONTENT_TYPE.equals(contentType)) {
                return extractPdf(fileData);
            }
            if (ResumeService.DOCX_CONTENT_TYPE.equals(contentType)) {
                return extractDocx(fileData);
            }
            throw new IllegalArgumentException("Only PDF and DOCX resumes can be analyzed.");
        } catch (IOException exception) {
            throw new IllegalArgumentException(
                    "The resume could not be read. Check that it is a valid PDF or DOCX file.",
                    exception);
        }
    }

    private String extractPdf(byte[] fileData) throws IOException {
        try (PDDocument document = Loader.loadPDF(fileData)) {
            return new PDFTextStripper().getText(document).trim();
        }
    }

    private String extractDocx(byte[] fileData) throws IOException {
        List<String> lines = new ArrayList<>();
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(fileData))) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText().trim();
                if (!text.isEmpty()) {
                    lines.add(text);
                }
            }
            for (XWPFTable table : document.getTables()) {
                for (var row : table.getRows()) {
                    List<String> cells = new ArrayList<>();
                    for (XWPFTableCell cell : row.getTableCells()) {
                        String text = cell.getText().trim();
                        if (!text.isEmpty()) {
                            cells.add(text);
                        }
                    }
                    if (!cells.isEmpty()) {
                        lines.add(String.join(" | ", cells));
                    }
                }
            }
        }
        return String.join("\n", lines).trim();
    }
}
