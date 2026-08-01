package com.example.careerpilot.parser;

import com.example.careerpilot.exception.ResumeParsingException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Component
public class PdfResumeParser implements ResumeParser {

    private static final int MINIMUM_USABLE_CHARACTERS = 30;

    @Override
    public String parse(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new ResumeParsingException(
                    "Resume PDF is empty."
            );
        }

        try {

            byte[] pdfBytes = file.getBytes();

            try (PDDocument document = Loader.loadPDF(pdfBytes)) {

                validateDocument(document);

                PDFTextStripper stripper =
                        new PDFTextStripper();

                stripper.setSortByPosition(true);

                String extractedText =
                        stripper.getText(document);

                String normalizedText =
                        normalize(extractedText);

                validateExtractedText(normalizedText);

                return normalizedText;
            }

        } catch (ResumeParsingException exception) {

            throw exception;

        } catch (IOException exception) {

            throw new ResumeParsingException(
                    "Unable to process the uploaded PDF. "
                            + "Please upload a valid text-based PDF resume.",
                    exception
            );
        }
    }

    private void validateDocument(PDDocument document) {

        if (document.getNumberOfPages() == 0) {
            throw new ResumeParsingException(
                    "The uploaded PDF contains no pages."
            );
        }

        if (document.isEncrypted()) {
            throw new ResumeParsingException(
                    "Password-protected or encrypted resumes are not supported."
            );
        }
    }

    private void validateExtractedText(String text) {

        if (text == null || text.isBlank()) {
            throw new ResumeParsingException(
                    "No readable text was found in the resume. "
                            + "Scanned or image-only PDFs are currently unsupported."
            );
        }

        long usableCharacters = text.chars()
                .filter(Character::isLetterOrDigit)
                .count();

        if (usableCharacters < MINIMUM_USABLE_CHARACTERS) {
            throw new ResumeParsingException(
                    "The resume does not contain enough readable text for analysis. "
                            + "Scanned or image-only PDFs are currently unsupported."
            );
        }
    }

    private String normalize(String text) {

        if (text == null) {
            return "";
        }

        return text
                // Normalize Windows/macOS line endings
                .replace("\r\n", "\n")
                .replace('\r', '\n')

                // Remove common invisible extraction characters
                .replace("\u0000", "")
                .replace("\u00A0", " ")

                // Normalize spaces/tabs without removing line structure
                .replaceAll("[\\t ]+", " ")

                // Remove spaces around line breaks
                .replaceAll(" *\\n *", "\n")

                // Avoid excessive blank lines
                .replaceAll("\\n{3,}", "\n\n")

                .trim();
    }
}