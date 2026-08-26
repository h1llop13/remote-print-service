package com.remoteprint.document;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

@Service
public class PdfService {

    public int getPageCount(String filePath) throws IOException {

        File file = new File(filePath);

        try (PDDocument document = Loader.loadPDF(file)) {
            return document.getNumberOfPages();
        }
    }

    public String createPrintablePdf(
            String originalFilePath,
            Set<Integer> selectedPages
    ) throws IOException {

        File originalFile = new File(originalFilePath);

        Path originalPath = Path.of(originalFilePath);
        Path printablePath = originalPath
                .getParent()
                .resolve("printable.pdf");

        try (
                PDDocument originalDocument = Loader.loadPDF(originalFile);
                PDDocument printableDocument = new PDDocument()
        ) {

            for (Integer pageNumber : selectedPages) {
                printableDocument.importPage(
                        originalDocument.getPage(pageNumber - 1)
                );
            }

            printableDocument.save(printablePath.toFile());
        }

        return printablePath.toString();
    }
}