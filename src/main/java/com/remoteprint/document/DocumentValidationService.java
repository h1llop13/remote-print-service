package com.remoteprint.document;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class DocumentValidationService {

    public void validatePdf(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "File must not be empty"
            );
        }

        String fileName = file.getOriginalFilename();

        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException(
                    "File name is missing"
            );
        }

        if (!fileName.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException(
                    "Only PDF files are supported"
            );
        }

        validatePdfStructure(file);
    }

    private void validatePdfStructure(MultipartFile file) {

        try (PDDocument document = Loader.loadPDF(file.getBytes())) {

            if (document.getNumberOfPages() < 1) {
                throw new IllegalArgumentException(
                        "PDF must contain at least one page"
                );
            }

        } catch (IOException exception) {
            throw new IllegalArgumentException(
                    "File is not a valid PDF"
            );
        }
    }
}