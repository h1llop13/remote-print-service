package com.remoteprint.job;

import com.remoteprint.print.PrinterService;
import com.remoteprint.print.PrintRequest;
import com.remoteprint.print.PrintResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PrintJobService {

    private static final Logger log =
            LoggerFactory.getLogger(PrintJobService.class);

    private final PrinterService printerService;

    public PrintJobService(PrinterService printerService) {
        this.printerService = printerService;
    }

    public PrintJob createJob(
            String originalFileName,
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

            log.info(
                    "Processing print job {}",
                    job.getId()
            );

            if (!printerService.isPrinterAvailable()) {
                job.setStatus(PrintJobStatus.FAILED);
                job.setErrorMessage("Printer is unavailable");

                log.warn(
                        "Print job {} failed: printer is unavailable",
                        job.getId()
                );

                return job;
            }

            job.setStatus(PrintJobStatus.PRINTING);

            PrintRequest printRequest = new PrintRequest(
                    job.getPrintableFilePath(),
                    job.getCopies()
            );

            PrintResult printResult =
                    printerService.print(printRequest);

            if (!printResult.isSuccess()) {
                job.setStatus(PrintJobStatus.FAILED);
                job.setErrorMessage(printResult.getMessage());

                log.warn(
                        "Print job {} failed: {}",
                        job.getId(),
                        printResult.getMessage()
                );

                return job;
            }

            job.setStatus(PrintJobStatus.COMPLETED);
            job.setCompletedAt(LocalDateTime.now());

            log.info(
                    "Print job {} completed",
                    job.getId()
            );

            return job;

        } catch (Exception exception) {

            job.setStatus(PrintJobStatus.FAILED);
            job.setErrorMessage(exception.getMessage());

            log.error(
                    "Print job {} failed",
                    job.getId(),
                    exception
            );

            return job;
        }
    }
}