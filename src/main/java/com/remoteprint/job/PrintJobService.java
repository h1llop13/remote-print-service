package com.remoteprint.job;

import com.remoteprint.print.PrinterService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PrintJobService {

    private final PrinterService printerService;

    public PrintJobService(PrinterService printerService) {
        this.printerService = printerService;
    }

    public PrintJob createJob(String originalFileName,
                              String fileType
                            ) {
        PrintJob job = new PrintJob();

        job.setId(UUID.randomUUID());
        job.setOriginalFileName(originalFileName);
        job.setFileType(fileType);
        job.setStatus(PrintJobStatus.RECEIVED);
        job.setPageRange("ALL");
        job.setCopies(1);
        job.setCreatedAt(LocalDateTime.now());

        return job;
    }

    public PrintJob processJob(PrintJob job) {
        try {
            job.setStatus(PrintJobStatus.PROCESSING);
            job.setStartedAt(LocalDateTime.now());

            if(!printerService.isPrinterAvailable()) {
                job.setStatus(PrintJobStatus.FAILED);
                job.setErrorMessage("Printer is unavailable");
                return job;
            }

            job.setStatus(PrintJobStatus.PRINTING);

            job.setStatus(PrintJobStatus.COMPLETED);
            job.setCompletedAt(LocalDateTime.now());

            return job;
        } catch (Exception exception) {
            job.setStatus(PrintJobStatus.FAILED);
            job.setErrorMessage(exception.getMessage());

            return job;
        }
    }
}
