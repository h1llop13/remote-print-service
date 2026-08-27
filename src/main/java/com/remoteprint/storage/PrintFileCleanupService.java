package com.remoteprint.storage;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;

@Service
public class PrintFileCleanupService {

    private static final Logger log =
            LoggerFactory.getLogger(PrintFileCleanupService.class);

    private static final Path PRINT_JOBS_DIRECTORY =
            Path.of("data", "print-jobs");

    private static final Duration MAX_FILE_AGE =
            Duration.ofDays(7);

    @PostConstruct
    public void cleanupOnStartup() {

        if (!Files.exists(PRINT_JOBS_DIRECTORY)) {
            return;
        }

        Instant threshold =
                Instant.now().minus(MAX_FILE_AGE);

        try (var directories =
                     Files.list(PRINT_JOBS_DIRECTORY)) {

            directories
                    .filter(Files::isDirectory)
                    .forEach(directory ->
                            deleteIfExpired(
                                    directory,
                                    threshold
                            )
                    );

        } catch (IOException exception) {

            log.error(
                    "Failed to scan print job directory",
                    exception
            );
        }
    }

    private void deleteIfExpired(
            Path directory,
            Instant threshold
    ) {

        try {
            FileTime modified =
                    Files.getLastModifiedTime(directory);

            if (modified.toInstant().isAfter(threshold)) {
                return;
            }

            try (var paths = Files.walk(directory)) {

                paths.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException exception) {
                                log.warn(
                                        "Failed to delete {}",
                                        path,
                                        exception
                                );
                            }
                        });
            }

            log.info(
                    "Deleted expired print files: {}",
                    directory
            );

        } catch (IOException exception) {

            log.warn(
                    "Failed to clean print directory: {}",
                    directory,
                    exception
            );
        }
    }
}