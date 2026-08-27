package com.remoteprint.dto;

import com.remoteprint.job.PrintJobStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record PrintJobResponse(
        UUID id,
        String originalFileName,
        String fileType,
        String originalFilePath,
        String printableFilePath,
        PrintJobStatus status,
        String pageRange,
        int copies,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        String errorMessage
) {
}