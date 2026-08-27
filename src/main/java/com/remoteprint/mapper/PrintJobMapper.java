package com.remoteprint.mapper;

import com.remoteprint.dto.PrintJobResponse;
import com.remoteprint.job.PrintJob;
import org.springframework.stereotype.Component;

@Component
public class PrintJobMapper {

    public PrintJobResponse toResponse(PrintJob job) {
        return new PrintJobResponse(
                job.getId(),
                job.getOriginalFileName(),
                job.getFileType(),
                job.getOriginalFilePath(),
                job.getPrintableFilePath(),
                job.getStatus(),
                job.getPageRange(),
                job.getCopies(),
                job.getCreatedAt(),
                job.getStartedAt(),
                job.getCompletedAt(),
                job.getErrorMessage()
        );
    }
}
