package com.remoteprint.document;

import com.remoteprint.config.DocumentProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Service
public class DocumentConverter {

    private final DocumentProperties documentProperties;

    public DocumentConverter(
            DocumentProperties documentProperties
    ) {
        this.documentProperties = documentProperties;
    }

    public String convertToPdf(
            String inputFilePath,
            String outputDirectory
    ) throws IOException {

        Path outputPath = Path.of(outputDirectory);

        Files.createDirectories(outputPath);

        ProcessBuilder processBuilder = new ProcessBuilder(
                documentProperties.getLibreofficePath(),
                "--headless",
                "--convert-to",
                "pdf",
                "--outdir",
                outputPath.toAbsolutePath().toString(),
                Path.of(inputFilePath).toAbsolutePath().toString()
        );

        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        try {
            boolean finished = process.waitFor(
                    60,
                    TimeUnit.SECONDS
            );

            if (!finished) {
                process.destroyForcibly();

                throw new IllegalStateException(
                        "Document conversion timed out"
                );
            }

            if (process.exitValue() != 0) {
                throw new IllegalStateException(
                        "LibreOffice conversion failed"
                );
            }

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Document conversion interrupted",
                    exception
            );
        }

        String inputFileName =
                Path.of(inputFilePath)
                        .getFileName()
                        .toString();

        int extensionIndex =
                inputFileName.lastIndexOf('.');

        String baseName =
                extensionIndex > 0
                        ? inputFileName.substring(0, extensionIndex)
                        : inputFileName;

        Path convertedPdf =
                outputPath.resolve(
                        baseName + ".pdf"
                );

        if (!Files.exists(convertedPdf)) {
            throw new IllegalStateException(
                    "Converted PDF was not created"
            );
        }

        return convertedPdf.toString();
    }
}