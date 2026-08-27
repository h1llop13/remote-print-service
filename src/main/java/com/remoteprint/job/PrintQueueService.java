package com.remoteprint.job;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public class PrintQueueService {

    private static final Logger log =
            LoggerFactory.getLogger(PrintQueueService.class);

    private final BlockingQueue<PrintJob> queue =
            new LinkedBlockingQueue<>();

    private final PrintJobService printJobService;

    public PrintQueueService(
            PrintJobService printJobService
    ) {
        this.printJobService = printJobService;
    }

    @PostConstruct
    public void start() {

        restoreJobs();

        Thread worker = new Thread(
                this::processQueue,
                "print-queue-worker"
        );

        worker.setDaemon(true);
        worker.start();

        log.info("Print queue worker started");
    }

    public void enqueue(PrintJob job) {

        job.setStatus(PrintJobStatus.QUEUED);
        printJobService.save(job);

        queue.offer(job);

        log.info(
                "Print job {} added to queue",
                job.getId()
        );
    }

    private void restoreJobs() {

        var jobs =
                printJobService.findRecoverableJobs();

        for (PrintJob job : jobs) {

            job.setStatus(PrintJobStatus.QUEUED);
            printJobService.save(job);

            queue.offer(job);
        }

        if (!jobs.isEmpty()) {
            log.info(
                    "Restored {} print jobs after startup",
                    jobs.size()
            );
        }
    }

    private void processQueue() {

        while (!Thread.currentThread().isInterrupted()) {

            try {
                PrintJob job = queue.take();

                log.info(
                        "Processing queued print job {}",
                        job.getId()
                );

                PrintJob processedJob =
                        printJobService.processJob(job);

                printJobService.save(processedJob);

            } catch (InterruptedException exception) {

                Thread.currentThread().interrupt();

                log.info("Print queue worker interrupted");

            } catch (Exception exception) {

                log.error(
                        "Unexpected print queue error",
                        exception
                );
            }
        }
    }
}