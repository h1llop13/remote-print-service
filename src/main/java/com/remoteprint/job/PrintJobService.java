package com.remoteprint.job;

import com.remoteprint.exception.PrintJobNotFoundException;
import com.remoteprint.print.PrinterService;
import com.remoteprint.print.PrintRequest;
import com.remoteprint.print.PrintResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PrintJobService {

    private static final Logger log =
            LoggerFactory.getLogger(PrintJobService.class);

    private final PrinterService printerService;
    private final PrintJobRepository printJobRepository;

    public PrintJobService(
            PrinterService printerService,
            PrintJobRepository printJobRepository
    ) {
        this.printerService = printerService;
        this.printJobRepository = printJobRepository;
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

        printJobRepository.save(job);

        return job;
    }

    public PrintJob processJob(PrintJob job) {

        try {
            job.setStatus(PrintJobStatus.PROCESSING);
            job.setStartedAt(LocalDateTime.now());
            job.setErrorMessage(null);

            printJobRepository.save(job);

            log.info(
                    "Processing print job {}",
                    job.getId()
            );

            if (!printerService.isPrinterAvailable()) {

                job.setStatus(PrintJobStatus.FAILED);
                job.setErrorMessage("Printer is unavailable");

                printJobRepository.save(job);

                log.warn(
                        "Print job {} failed: printer is unavailable",
                        job.getId()
                );

                return job;
            }

            job.setStatus(PrintJobStatus.PRINTING);

            printJobRepository.save(job);

            PrintRequest printRequest =
                    new PrintRequest(
                            job.getPrintableFilePath(),
                            job.getCopies()
                    );

            PrintResult printResult =
                    printerService.print(printRequest);

            if (!printResult.isSuccess()) {

                job.setStatus(PrintJobStatus.FAILED);
                job.setErrorMessage(
                        printResult.getMessage()
                );

                printJobRepository.save(job);

                log.warn(
                        "Print job {} failed: {}",
                        job.getId(),
                        printResult.getMessage()
                );

                return job;
            }

            job.setStatus(PrintJobStatus.COMPLETED);
            job.setCompletedAt(LocalDateTime.now());
            job.setErrorMessage(null);

            printJobRepository.save(job);

            log.info(
                    "Print job {} completed",
                    job.getId()
            );

            return job;

        } catch (Exception exception) {

            job.setStatus(PrintJobStatus.FAILED);
            job.setErrorMessage(
                    exception.getMessage()
            );

            printJobRepository.save(job);

            log.error(
                    "Print job {} failed",
                    job.getId(),
                    exception
            );

            return job;
        }
    }

    public PrintJob getJob(UUID id) {

        return printJobRepository
                .findById(id)
                .orElseThrow(
                        () -> new PrintJobNotFoundException(id)
                );
    }

    public List<PrintJob> getJobs() {
        return printJobRepository.findAll();
    }

    public PrintJob retryJob(UUID id) {

        PrintJob job = getJob(id);

        if (job.getStatus() != PrintJobStatus.FAILED) {
            throw new IllegalStateException(
                    "Only failed print jobs can be retried"
            );
        }

        job.setStatus(PrintJobStatus.RECEIVED);
        job.setStartedAt(null);
        job.setCompletedAt(null);
        job.setErrorMessage(null);

        printJobRepository.save(job);

        log.info(
                "Print job {} prepared for retry",
                job.getId()
        );

        return job;
    }

    public PrintJob save(PrintJob job) {
        return printJobRepository.save(job);
    }

    public List<PrintJob> findRecoverableJobs() {

        return printJobRepository
                .findByStatusInOrderByCreatedAtAsc(
                        List.of(
                                PrintJobStatus.QUEUED,
                                PrintJobStatus.PROCESSING,
                                PrintJobStatus.PRINTING
                        )
                );
    }
}