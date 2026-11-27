package com.jobtracker.service;

import com.jobtracker.exception.AiServiceUnavailableException;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class ResumeDocumentWriter {
    private final String pdfFontPath;

    public ResumeDocumentWriter(
            @Value("${app.resume.pdf-font-path:}") String pdfFontPath) {
        this.pdfFontPath = pdfFontPath;
    }

    public byte[] write(String text, String contentType) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Revised resume text cannot be empty.");
        }
        try {
            if (ResumeService.PDF_CONTENT_TYPE.equals(contentType)) {
                return writePdf(text);
            }
            if (ResumeService.DOCX_CONTENT_TYPE.equals(contentType)) {
                return writeDocx(text);
            }
            throw new IllegalArgumentException("Only PDF and DOCX resumes can be generated.");
        } catch (IOException exception) {
            throw new AiServiceUnavailableException(
                    "The revised resume could not be created as a document.", exception);
        }
    }

    private byte[] writeDocx(String text) throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (String line : cleanText(text).split("\\R", -1)) {
                XWPFParagraph paragraph = document.createParagraph();
                paragraph.createRun().setText(line);
            }
            document.write(output);
            return output.toByteArray();
        }
    }

    private byte[] writePdf(String text) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDFont font = loadPdfFont(document);
            List<String> lines = wrap(cleanText(text), 92);
            int linesPerPage = (int) ((PDRectangle.A4.getHeight() - 96) / 13);
            for (int start = 0; start < lines.size(); start += linesPerPage) {
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                    stream.beginText();
                    try {
                        stream.setFont(font, 10);
                        stream.newLineAtOffset(48, page.getMediaBox().getHeight() - 48);
                        int end = Math.min(start + linesPerPage, lines.size());
                        for (int index = start; index < end; index++) {
                            String line = toPdfText(lines.get(index));
                            try {
                                font.getStringWidth(line);
                            } catch (IllegalArgumentException exception) {
                                throw new IllegalArgumentException(
                                        "The current PDF font cannot display this resume. "
                                                + "Set RESUME_PDF_FONT_PATH to a TrueType font that "
                                                + "supports the resume's language.", exception);
                            }
                            stream.showText(line);
                            stream.newLineAtOffset(0, -13);
                        }
                    } finally {
                        stream.endText();
                    }
                }
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    private PDFont loadPdfFont(PDDocument document) throws IOException {
        if (pdfFontPath == null || pdfFontPath.isBlank()) {
            return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        }
        File fontFile = new File(pdfFontPath.trim());
        if (!fontFile.isFile()) {
            throw new AiServiceUnavailableException(
                    "RESUME_PDF_FONT_PATH must point to an available TrueType font file.");
        }
        return PDType0Font.load(document, fontFile);
    }

    private List<String> wrap(String text, int maxLength) {
        List<String> result = new ArrayList<>();
        for (String originalLine : text.split("\\R", -1)) {
            if (originalLine.isBlank()) {
                result.add("");
                continue;
            }
            String remaining = originalLine;
            while (remaining.length() > maxLength) {
                int splitAt = safeSplitAt(remaining, maxLength);
                result.add(remaining.substring(0, splitAt));
                remaining = remaining.substring(splitAt).stripLeading();
            }
            result.add(remaining);
        }
        return result;
    }

    private int safeSplitAt(String text, int maxLength) {
        int splitAt = text.lastIndexOf(' ', maxLength);
        if (splitAt < 1) {
            splitAt = maxLength;
        }
        if (splitAt < text.length() && splitAt > 0
                && Character.isHighSurrogate(text.charAt(splitAt - 1))
                && Character.isLowSurrogate(text.charAt(splitAt))) {
            splitAt--;
        }
        return splitAt;
    }

    private String cleanText(String text) {
        return text.replaceAll("(?m)^#{1,6}\\s*", "")
                .replace("**", "")
                .replace("__", "")
                .replace("```", "")
                .trim();
    }

    private String toPdfText(String text) {
        return text.replace('•', '-')
                .replace('–', '-')
                .replace('—', '-')
                .replace('’', '\'')
                .replace('“', '"')
                .replace('”', '"');
    }
}
