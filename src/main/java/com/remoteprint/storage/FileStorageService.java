package com.remoteprint.storage;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path storageRoot = Path.of("data", "print-jobs");

    public String saveOriginalFile(UUID jobId, MultipartFile file) throws IOException {

        Path jobDirectory = storageRoot.resolve(jobId.toString());

        Files.createDirectories(jobDirectory);

        Path targetFile = jobDirectory.resolve("original.pdf");

        try (var inputStream = file.getInputStream()) {
            Files.copy(
                    inputStream,
                    targetFile,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }

        return targetFile.toString();
    }

    public String savePrintableSourceFile(
            UUID jobId,
            MultipartFile file
    ) throws IOException {

        Path jobDirectory =
                Path.of(
                        "data",
                        "print-jobs",
                        jobId.toString()
                );

        Files.createDirectories(jobDirectory);

        Path destination =
                jobDirectory.resolve("source.pdf");

        file.transferTo(destination);

        return destination.toString();
    }
}